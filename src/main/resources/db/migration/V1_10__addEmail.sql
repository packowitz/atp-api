ALTER TABLE "user" ADD COLUMN email VARCHAR(100) UNIQUE;
ALTER TABLE "user" ADD COLUMN email_confirmed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE "user" DROP CONSTRAINT IF EXISTS user_username_key;
DROP INDEX IF EXISTS public.user_username_uindex RESTRICT;

CREATE TABLE email_confirmation (
  id SERIAL,
  email VARCHAR(100) NOT NULL,
  user_id BIGINT NOT NULL,
  confirmation_send_date TIMESTAMPTZ,
  confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  confirmation_date TIMESTAMPTZ,
  PRIMARY KEY(id)
);