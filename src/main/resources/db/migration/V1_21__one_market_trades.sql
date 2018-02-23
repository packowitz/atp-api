CREATE TABLE trade_one_market (
  id SERIAL,
  plan_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  min_profit DOUBLE PRECISION NOT NULL,
  start_currency VARCHAR(10) NOT NULL,
  start_amount DOUBLE PRECISION NOT NULL,
  balance DOUBLE PRECISION NOT NULL,
  auto_restart BOOLEAN NOT NULL,
  start_date TIMESTAMPTZ,
  finish_date TIMESTAMPTZ,
  PRIMARY KEY (id)
);

DROP TABLE trade_order_observer;
DROP TABLE trade_circle;
DELETE FROM trade_path;
DELETE  FROM trade_plan;

ALTER TABLE trade_path ADD COLUMN account_id BIGINT NOT NULL DEFAULT 0;

DROP TABLE trade_step;

CREATE TABLE trade_step (
  id SERIAL,
  plan_id BIGINT NOT NULL,
  subplan_id BIGINT NOT NULL,
  step INT NOT NULL,
  order_id BIGINT,
  order_filled DOUBLE PRECISION NOT NULL,
  order_altcoin_qty DOUBLE PRECISION,
  order_basecoin_qty DOUBLE PRECISION,
  status VARCHAR(10) NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  side VARCHAR(10) NOT NULL,
  price DOUBLE PRECISION NOT NULL,
  price_threshold DOUBLE PRECISION,
  in_currency VARCHAR(10),
  in_amount DOUBLE PRECISION,
  in_filled DOUBLE PRECISION NOT NULL,
  out_currency VARCHAR(10),
  out_amount DOUBLE PRECISION NOT NULL,
  start_date TIMESTAMPTZ,
  finish_date TIMESTAMPTZ,
  PRIMARY KEY (id)
);