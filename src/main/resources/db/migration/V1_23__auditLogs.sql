CREATE TABLE trade_audit_log (
  id SERIAL,
  plan_id BIGINT NOT NULL,
  subplan_id BIGINT NOT NULL,
  step_id BIGINT,
  timestamp TIMESTAMPTZ,
  level VARCHAR(10),
  title VARCHAR(150),
  message VARCHAR(511),
  PRIMARY KEY (id)
);