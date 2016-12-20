CREATE TABLE in_app_purchase (
  id SERIAL,
  user_id BIGINT NOT NULL,
  os VARCHAR(15) NOT NULL,
  consumed BOOLEAN NOT NULL,
  buy_date TIMESTAMPTZ NOT NULL,
  consume_date TIMESTAMPTZ,
  product_id VARCHAR(25) NOT NULL,
  reward INT NOT NULL,
  receipt VARCHAR(2000) NOT NULL,
  PRIMARY KEY(id)
);