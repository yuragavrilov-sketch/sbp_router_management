ALTER TABLE traffic_transactions ADD COLUMN operation_id   varchar(128);
ALTER TABLE traffic_transactions ADD COLUMN operation_type varchar(16);
CREATE INDEX idx_traffic_operation_id ON traffic_transactions (operation_id);
