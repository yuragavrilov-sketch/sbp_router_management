ALTER TABLE traffic_transactions ADD COLUMN fault_string varchar(2000);

ALTER TABLE traffic_transactions
    ALTER COLUMN status TYPE varchar(32);

ALTER TABLE traffic_transactions
    DROP CONSTRAINT traffic_transactions_status_chk;

ALTER TABLE traffic_transactions
    ADD CONSTRAINT traffic_transactions_status_chk
        CHECK (status IN ('PENDING', 'RESPONDED', 'RESPONDED_WITH_ERROR'));
