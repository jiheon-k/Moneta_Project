# 거래 계좌 구조 변경

`account`는 모든 거래의 기준 계좌이며, 이체에서는 출금 계좌다.
`transferAccount`는 이체받는 계좌이며 수입·지출에서는 null이다.
API 응답도 `accountId`, `transferAccountId`로 변경했다.

| 유형 | account_id | transfer_account_id |
|---|---|---|
| INCOME | 기존 to_account_id | NULL |
| EXPENSE | 기존 from_account_id | NULL |
| TRANSFER | 기존 from_account_id | 기존 to_account_id |

## 기존 MySQL DB 이전

이 SQL은 자동 실행되지 않는다. `ddl-auto=update`는 기존 값의 의미를 바꿔 옮기지 않는다.
기존 데이터가 있다면 **변경된 앱을 시작하기 전에** 아래 순서로 이전한다.
앱을 중지하고 DB를 백업한 뒤, 실제 테이블명이 `transaction`, `account`인지 확인한다.

1. 아래 검사 쿼리를 먼저 실행한다. 결과가 있으면 해당 데이터를 확인·수정한 뒤 진행한다.
2. `transaction-account-migration.sql`을 기존 DB에 한 번 실행한다.
   새 컬럼이 이미 생성되어 있다면 이 파일을 그대로 재실행하지 말고 현재 스키마를 먼저 확인한다.
3. 기존 수입·지출·이체 건수, 잔액, 계좌별 거래 조회 결과를 비교한다.
4. 앱을 시작한다. 기존 컬럼은 확인과 복구를 위해 남겨 두며 앱에서는 사용하지 않는다.

MySQL의 ALTER TABLE은 자동 커밋되므로 전체 SQL을 한 번에 롤백할 수 없다.

```sql
SELECT id, type, from_account_id, to_account_id
FROM `transaction`
WHERE type IS NULL
   OR type NOT IN ('INCOME', 'EXPENSE', 'TRANSFER')
   OR (type = 'INCOME' AND (to_account_id IS NULL OR from_account_id IS NOT NULL))
   OR (type = 'EXPENSE' AND (from_account_id IS NULL OR to_account_id IS NOT NULL))
   OR (type = 'TRANSFER' AND
       (from_account_id IS NULL OR to_account_id IS NULL OR from_account_id = to_account_id));
```

데이터가 없는 새 DB는 JPA가 새 매핑으로 테이블을 생성하므로 이전 SQL이 필요 없다.
