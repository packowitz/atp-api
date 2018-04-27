CREATE TABLE trade_plan_config (
  plan_id BIGINT NOT NULL,
  auto_restart BOOLEAN NOT NULL,
  start_currency VARCHAR(10) NOT NULL,
  start_amount DOUBLE PRECISION NOT NULL,
  first_market_strategy VARCHAR(50) NOT NULL,
  first_market_strategy_params VARCHAR(50),
  first_step_price_strategy VARCHAR(50) NOT NULL,
  first_step_price_strategy_params VARCHAR(50),
  next_market_strategy VARCHAR(50) NOT NULL,
  next_market_strategy_params VARCHAR(50),
  PRIMARY KEY (plan_id)
);

ALTER TABLE trade_plan ADD COLUMN balance DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE trade_plan ADD COLUMN finish_date TIMESTAMPTZ;
