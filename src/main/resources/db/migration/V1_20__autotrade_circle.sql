CREATE TABLE trade_plan (
  id SERIAL,
  user_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  plan_type VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE trade_circle (
  id SERIAL,
  plan_id BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL,
  active_step INT,
  active_order_id BIGINT,
  start_currency VARCHAR(10) NOT NULL,
  start_amount DOUBLE PRECISION NOT NULL,
  finish_amount DOUBLE PRECISION,
  risk VARCHAR(50),
  treshold INT NOT NULL,
  cancel_on_treshold BOOLEAN NOT NULL,
  start_date TIMESTAMPTZ,
  finish_date TIMESTAMPTZ,
  PRIMARY KEY (id)
);

CREATE TABLE trade_circle_step (
  id SERIAL,
  circle_id BIGINT,
  step INT NOT NULL,
  order_id BIGINT,
  status VARCHAR(10) NOT NULL ,
  symbol VARCHAR(20) NOT NULL,
  side VARCHAR(10) NOT NULL,
  price DOUBLE PRECISION NOT NULL,
  in_currency VARCHAR(10),
  in_amount DOUBLE PRECISION,
  out_currency VARCHAR(10),
  out_amount DOUBLE PRECISION,
  start_date TIMESTAMPTZ,
  finish_date TIMESTAMPTZ,
  PRIMARY KEY (id)
);

CREATE TABLE trade_order_observer (
  id SERIAL,
  order_id BIGINT NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  broker VARCHAR(50) NOT NULL,
  user_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  plan_type VARCHAR(50) NOT NULL,
  subplan_id BIGINT NOT NULL,
  treshold INT NOT NULL,
  cancel_on_treshold BOOLEAN NOT NULL,
  check_date TIMESTAMPTZ,
  PRIMARY KEY (id)
);