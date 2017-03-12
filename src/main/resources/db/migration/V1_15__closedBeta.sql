CREATE TABLE closed_beta (
  id SERIAL,
  gmail VARCHAR(100),
  apple_id VARCHAR(100),
  finding VARCHAR(255),
  register_date TIMESTAMPTZ,
  gmail_send_date TIMESTAMPTZ,
  apple_send_date TIMESTAMPTZ,
  PRIMARY KEY(id)
);