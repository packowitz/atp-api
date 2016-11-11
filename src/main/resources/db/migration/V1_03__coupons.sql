CREATE TABLE coupon (
  id SERIAL,
  admin_id BIGINT NOT NULL,
  creation_date TIMESTAMPTZ NOT NULL,
  code VARCHAR(25) UNIQUE NOT NULL,
  active BOOLEAN NOT NULL,
  reward INT NOT NULL,
  single_use BOOLEAN NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  redeemed INT NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE coupon_redeem (
  id SERIAL,
  coupon_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  redeem_date TIMESTAMPTZ NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE user_rights (
  user_id BIGINT NOT NULL,
  callcenter BOOLEAN DEFAULT FALSE,
  marketing BOOLEAN DEFAULT FALSE,
  user_admin BOOLEAN DEFAULT FALSE,
  security BOOLEAN DEFAULT FALSE,
  coupons BOOLEAN DEFAULT FALSE,
  PRIMARY KEY(user_id)
);

INSERT INTO user_rights VALUES (1, TRUE, TRUE, TRUE, TRUE, TRUE);

ALTER TABLE "user" DROP COLUMN right_callcenter;
ALTER TABLE "user" DROP COLUMN right_marketing;
ALTER TABLE "user" DROP COLUMN right_user_admin;
ALTER TABLE "user" DROP COLUMN right_security;