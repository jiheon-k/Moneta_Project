package com.jiheon.moneta.category;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.jiheon.moneta.transaction.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import com.jiheon.moneta.category.dto.CategoryRequest;
import com.jiheon.moneta.category.dto.CategoryResponse;
import com.jiheon.moneta.user.User;
import com.jiheon.moneta.user.UserRepository;

// 공통·개인 카테고리를 조회하고 개인 항목의 변경 권한을 확인한다.
@Service
public class CategoryService {
    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionRepository transactionRepo;

    public CategoryService(CategoryRepository categoryRepo, UserRepository userRepo, TransactionRepository transactionRepo) {
        this.categoryRepo = categoryRepo;
        this.userRepo = userRepo;
        this.transactionRepo = transactionRepo;
    }

    // 카테고리 목록 조회
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryList(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자가 없습니다."));
        // 공통 카테고리와 사용자가 추가한 카테고리 가져오기
        List<Category> list = categoryRepo.findByUserIsNullOrUser_Id(userId);
        List<CategoryResponse> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Category category = list.get(i);
            result.add(new CategoryResponse(category.getName(), category.getId(), category.getUser() == null));
        }
        return result;
    }

    // 사용자 지정 카테고리 추가
    public void addNewCateogory(Long userId, CategoryRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자가 없습니다."));

        Category c = new Category();
        c.setName(request.getName());
        c.setUser(user);
        categoryRepo.save(c);
    }

    // 사용자가 추가했던 카테고리 이름 수정
    @Transactional
    public void changeCategory(Long userId, Long id, CategoryRequest request) {
        Category c = categoryRepo.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "수정할 카테고리가 없습니다."));

        c.setName(request.getName());
        categoryRepo.save(c);
    }

    // 사용자가 추가했던 카테고리 삭제
    @Transactional
    public void deleteCategory(Long userId, Long id) {
        Category c = categoryRepo.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "삭제할 카테고리가 없습니다."));
        if (transactionRepo.existsByCategory_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "거래가 있는 카테고리는 삭제할 수 없습니다.");
        }
        try {
            categoryRepo.delete(c);
            categoryRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 기록에서 사용 중인 카테고리는 삭제할 수 없습니다.");
        }
    }
}
