-- 과거 설계 기록: 현재 설계에는 적용하지 않는다.
-- 거래 소유자는 계좌 소유자를 기준으로 판단하며 Transaction.user는 제거했다.
-- 사용자가 거래 0건 확인 후 user_id와 fk_transaction_user를 삭제한 결과를 공유했다.
-- 아래 SQL은 과거에 사용자 컬럼을 추가하던 절차이며 재실행하면 안 된다.
ALTER TABLE `transaction`
    ADD COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES `user`(id);
