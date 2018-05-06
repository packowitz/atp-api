ALTER TABLE trade_step ALTER COLUMN subplan_id DROP NOT NULL;
ALTER TABLE trade_audit_log ALTER COLUMN subplan_id DROP NOT NULL;

DROP TABLE trade_one_market;