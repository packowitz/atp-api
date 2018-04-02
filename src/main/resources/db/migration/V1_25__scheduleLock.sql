CREATE TABLE trade_schedule_lock (
  id VARCHAR(30),
  locked BOOLEAN NOT NULL DEFAULT FALSE,
  started_timestamp TIMESTAMPTZ,
  finished_timestamp TIMESTAMPTZ,
  PRIMARY KEY (id)
);