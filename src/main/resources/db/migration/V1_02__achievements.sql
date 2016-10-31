CREATE TABLE achievement (
  id SERIAL,
  user_id BIGINT NOT NULL,
  type VARCHAR(25) NOT NULL,
  achieved INT NOT NULL,
  claimed INT NOT NULL,
  last_claimed TIMESTAMPTZ,
  PRIMARY KEY(id)
);