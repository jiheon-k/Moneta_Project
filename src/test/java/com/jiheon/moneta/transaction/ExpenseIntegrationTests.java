package com.jiheon.moneta.transaction;

import com.jiheon.moneta.auth.dto.CsrfResponse;
import com.jiheon.moneta.category.Category;
import com.jiheon.moneta.category.CategoryRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 전체 앱을 테스트 전용 H2로 실행한다. 개발 MySQL에는 연결하지 않는다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expense-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class ExpenseIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper mapper;
    @Autowired private CategoryRepository categories;
    @Autowired private TransactionRepository transactions;

    private record Client(MockHttpSession session, String header, String token) {}

    private Client csrf(MockHttpSession session) throws Exception {
        var request = get("/auth/csrf");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var token = mapper.readValue(result.getResponse().getContentAsString(), CsrfResponse.class);
        return new Client((MockHttpSession) result.getRequest().getSession(), token.headerName(), token.token());
    }

    private ResultActions write(Client client, String method, String path, Map<String, ?> body) throws Exception {
        var request = switch (method) {
            case "PATCH" -> patch(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> post(path);
        };
        return mvc.perform(request.session(client.session()).header(client.header(), client.token())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private Client login() throws Exception {
        Client client = csrf(null);
        String loginId = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        var credentials = Map.of("loginId", loginId, "password", "a long test passphrase");
        var signup = new HashMap<String, Object>(credentials);
        signup.put("name", "사용자"); signup.put("age", 25);
        write(client, "POST", "/auth/signup", signup).andExpect(status().isCreated());
        write(client, "POST", "/auth/login", credentials).andExpect(status().isOk());
        return csrf(client.session());
    }

    private long account(Client client) throws Exception {
        return Long.parseLong(write(client, "POST", "/accounts", Map.of("account_name", "생활비",
                "account_type", "LIVING", "initialBalance", 10000))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Map<String, Object> expense(long id) {
        return new HashMap<>(Map.of("accountId", id, "amount", 3000,
                "transactionDate", "2026-01-15", "memo", "점심"));
    }

    @Test
    void signupToExpenseHistoryAndBalanceWorks() throws Exception {
        Client client = login();
        long id = account(client);
        mvc.perform(get("/accounts").session(client.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/accounts/" + id).session(client.session()))
                .andExpect(jsonPath("$.balance").value(10000));
        write(client, "POST", "/expenses", expense(id)).andExpect(status().isCreated());
        mvc.perform(get("/accounts/" + id).session(client.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(7000));
        mvc.perform(get("/accounts/" + id + "/transactions?start=2026-01-15&end=2026-01-16&type=EXPENSE")
                .session(client.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.tList.length()").value(1))
                .andExpect(jsonPath("$.tList[0].amount").value(3000))
                .andExpect(jsonPath("$.tList[0].memo").value("점심"));
        mvc.perform(get("/accounts/" + id + "/transactions?start=2026-01-14&end=2026-01-15")
                .session(client.session())).andExpect(jsonPath("$.tList.length()").value(0));
        write(client, "POST", "/auth/logout", Map.of()).andExpect(status().isNoContent());
        mvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void expenseCanBeReadMovedAndDeletedWithBalancesUpdated() throws Exception {
        Client owner = login();
        long originalAccount = account(owner);
        long destinationAccount = account(owner);
        var input = expense(originalAccount);
        Category category = new Category();
        category.setName("수정 전 분류");
        long categoryId = categories.save(category).getId();
        input.put("categoryId", categoryId);
        String id = write(owner, "POST", "/expenses", input).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String path = "/expenses/" + id;

        mvc.perform(get(path).session(owner.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(originalAccount))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.memo").value("점심"));

        write(owner, "PUT", path, Map.of("accountId", destinationAccount, "amount", 5000,
                "transactionDate", "2026-01-16")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.accountId").value(destinationAccount))
                .andExpect(jsonPath("$.amount").value(5000))
                .andExpect(jsonPath("$.transactionDate").value("2026-01-16"))
                .andExpect(jsonPath("$.memo").doesNotExist())
                .andExpect(jsonPath("$.categoryId").doesNotExist());
        expectBalance(owner, originalAccount, 10000);
        expectBalance(owner, destinationAccount, 5000);
        mvc.perform(get("/accounts/" + destinationAccount + "/transactions?start=2026-01-16&end=2026-01-17")
                .session(owner.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.tList[0].id").value(Long.parseLong(id)));

        write(owner, "DELETE", path, Map.of()).andExpect(status().isNoContent());
        expectBalance(owner, destinationAccount, 10000);
        mvc.perform(get(path).session(owner.session())).andExpect(status().isNotFound());
        write(owner, "DELETE", path, Map.of()).andExpect(status().isNotFound());
    }

    @Test
    void expenseChangesRejectInvalidInputAndOtherUsers() throws Exception {
        Client owner = login();
        Client other = login();
        long ownerAccount = account(owner);
        long otherAccount = account(other);
        String path = "/expenses/" + write(owner, "POST", "/expenses", expense(ownerAccount))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).session(other.session())).andExpect(status().isNotFound());
        write(other, "PUT", path, expense(otherAccount)).andExpect(status().isNotFound());
        write(other, "DELETE", path, Map.of()).andExpect(status().isNotFound());
        write(owner, "PUT", path, expense(otherAccount)).andExpect(status().isNotFound());
        write(owner, "PUT", path, Map.of("accountId", ownerAccount, "amount", 3000))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(path).session(owner.session())).andExpect(status().isForbidden());
        expectBalance(owner, ownerAccount, 7000);
    }

    private void expectBalance(Client client, long accountId, long balance) throws Exception {
        mvc.perform(get("/accounts/" + accountId).session(client.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    void invalidExpenseNeverChangesBalance() throws Exception {
        Client client = login();
        long id = account(client);
        for (Object amount : new Object[]{0, -1, 1.5, 1_000_000_000_001L}) {
            var input = expense(id); input.put("amount", amount);
            write(client, "POST", "/expenses", input).andExpect(status().isBadRequest());
        }
        var missing = expense(id); missing.remove("amount");
        write(client, "POST", "/expenses", missing).andExpect(status().isBadRequest());
        var future = expense(id); future.put("transactionDate", LocalDate.now().plusDays(1).toString());
        write(client, "POST", "/expenses", future).andExpect(status().isBadRequest());
        var memo = expense(id); memo.put("memo", "가".repeat(256));
        write(client, "POST", "/expenses", memo).andExpect(status().isBadRequest());
        mvc.perform(post("/expenses").session(client.session()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(expense(id)))).andExpect(status().isForbidden());
        assertEquals(0, transactions.sumOutgoingAmount(id));
    }

    @Test
    void anotherUsersAccountAndCategoryAreBlocked() throws Exception {
        Client owner = login(); Client other = login();
        long ownerAccount = account(owner); long otherAccount = account(other);
        write(owner, "POST", "/categories", Map.of("name", "개인 항목")).andExpect(status().isOk());
        long categoryId = mapper.readTree(mvc.perform(get("/categories").session(owner.session()))
                .andReturn().getResponse().getContentAsString()).valueStream()
                .filter(node -> node.get("name").asText().equals("개인 항목")).findFirst().orElseThrow().get("id").asLong();
        mvc.perform(get("/accounts").session(other.session()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(otherAccount));
        write(other, "POST", "/expenses", expense(ownerAccount)).andExpect(status().isNotFound());
        var input = expense(otherAccount); input.put("categoryId", categoryId);
        write(other, "POST", "/expenses", input).andExpect(status().isNotFound());
        write(other, "POST", "/expenses", expense(Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(get("/accounts/" + ownerAccount).session(other.session())).andExpect(status().isNotFound());
        write(other, "PATCH", "/accounts/" + ownerAccount, Map.of("account_name", "변경"))
                .andExpect(status().isNotFound());
        write(other, "DELETE", "/accounts/" + ownerAccount, Map.of()).andExpect(status().isNotFound());
        write(other, "DELETE", "/categories/" + categoryId, Map.of()).andExpect(status().isNotFound());
    }

    @Test
    void usedAccountAndCategoryArePreservedButNameCanChange() throws Exception {
        Client client = login(); long id = account(client);
        write(client, "PATCH", "/accounts/" + id, Map.of("initialBalance", 12000)).andExpect(status().isOk());
        write(client, "POST", "/categories", Map.of("name", "간식")).andExpect(status().isOk());
        long categoryId = mapper.readTree(mvc.perform(get("/categories").session(client.session()))
                .andReturn().getResponse().getContentAsString()).valueStream()
                .filter(node -> node.get("name").asText().equals("간식")).findFirst().orElseThrow().get("id").asLong();
        var input = expense(id); input.put("categoryId", categoryId);
        write(client, "POST", "/expenses", input).andExpect(status().isCreated());
        write(client, "PATCH", "/accounts/" + id, Map.of("initialBalance", 20000)).andExpect(status().isConflict());
        write(client, "DELETE", "/accounts/" + id, Map.of()).andExpect(status().isConflict());
        write(client, "DELETE", "/categories/" + categoryId, Map.of()).andExpect(status().isConflict());
        write(client, "PATCH", "/accounts/" + id, Map.of("account_name", "수정", "initialBalance", 12000))
                .andExpect(status().isOk());
        mvc.perform(get("/accounts/" + id).session(client.session()))
                .andExpect(jsonPath("$.name").value("수정")).andExpect(jsonPath("$.balance").value(9000));
        write(client, "DELETE", "/accounts/" + account(client), Map.of()).andExpect(status().isOk());
    }

    @Test
    void commonCategoryIsUsableButNotEditable() throws Exception {
        Category common = new Category(); common.setName("공통 식비");
        long categoryId = categories.save(common).getId();
        Client client = login();
        var input = expense(account(client)); input.put("categoryId", categoryId);
        write(client, "POST", "/expenses", input).andExpect(status().isCreated());
        write(client, "PATCH", "/categories/" + categoryId, Map.of("name", "변경"))
                .andExpect(status().isNotFound());
        write(client, "DELETE", "/categories/" + categoryId, Map.of()).andExpect(status().isNotFound());
    }
}
