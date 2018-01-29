CREATE TABLE trade_user (
  id SERIAL,
  username VARCHAR(100) UNIQUE,
  password VARCHAR(100),
  PRIMARY KEY(id)
);

CREATE TABLE trade_account (
  id SERIAL,
  user_id BIGINT NOT NULL,
  broker VARCHAR(50),
  api_key VARCHAR(255),
  private_key VARCHAR(255),
  PRIMARY KEY (id)
);