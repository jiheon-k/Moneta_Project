-- 실행 전 같은 폴더의 migration.md 검사 쿼리와 절차를 확인한다.
-- 기존 테이블에 새 컬럼이 아직 없을 때 한 번만 수동 실행한다.
ALTER TABLE `transaction`
    ADD COLUMN account_id BIGINT NULL,
    ADD COLUMN transfer_account_id BIGINT NULL;

UPDATE `transaction`
SET account_id = CASE
        WHEN type = 'INCOME' THEN to_account_id
        ELSE from_account_id
    END,
    transfer_account_id = CASE
        WHEN type = 'TRANSFER' THEN to_account_id
        ELSE NULL
    END;

ALTER TABLE `transaction`
    MODIFY COLUMN account_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account(id),
    ADD CONSTRAINT fk_transaction_transfer_account FOREIGN KEY (transfer_account_id) REFERENCES account(id);
