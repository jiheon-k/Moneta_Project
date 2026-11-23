package com.jiheon.moneta.transaction.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// 지출 등록 입력. 사용자와 거래 유형은 요청에서 받지 않고 서버에서 결정한다.
public record TransactionRequest(
        @NotNull @Positive Long accountId,
        @NotNull @Positive @Max(1_000_000_000_000L) Long amount,
        @NotNull @PastOrPresent LocalDate transactionDate,
        @Size(max = 255) String memo,
        @Positive Long categoryId) {
}
