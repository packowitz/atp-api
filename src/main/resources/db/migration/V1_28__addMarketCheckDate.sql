ALTER TABLE trade_step ADD COLUMN checked_market_date TIMESTAMPTZ;
ALTER TABLE trade_step ALTER COLUMN symbol DROP NOT NULL;
ALTER TABLE trade_one_market ALTER COLUMN symbol DROP NOT NULL;
ALTER TABLE trade_one_market ALTER COLUMN min_profit DROP NOT NULL;
ALTER TABLE trade_one_market ALTER COLUMN start_amount DROP NOT NULL;
ALTER TABLE trade_one_market ALTER COLUMN start_currency DROP NOT NULL;

INSERT INTO trade_schedule_lock VALUES ('PLAN_CHECK', FALSE);
INSERT INTO trade_schedule_lock VALUES ('PAUSED_PLAN_CHECK', FALSE);