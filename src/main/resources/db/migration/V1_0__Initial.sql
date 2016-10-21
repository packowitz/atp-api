CREATE TABLE "user" (
  id SERIAL,
  username VARCHAR(100) UNIQUE,
  password VARCHAR(100),
  yearOfBirth SMALLINT,
  country VARCHAR(3),
  male BOOLEAN,
  credits INT NOT NULL,
  reliable_score INT NOT NULL DEFAULT 100,
  surveys_answered BIGINT NOT NULL DEFAULT 0,
  surveys_started BIGINT NOT NULL DEFAULT 0,
  last_login_time TIMESTAMPTZ,
  survey_id_to_answer BIGINT,
  survey_expected_answer SMALLINT,
  survey_ask_time TIMESTAMPTZ,
  survey_type VARCHAR(25),
  device_os VARCHAR(25),
  notification_reg_id TEXT,
  notifications BOOLEAN NOT NULL DEFAULT TRUE,
  notifications_sound BOOLEAN NOT NULL DEFAULT FALSE,
  notifications_vibration BOOLEAN NOT NULL DEFAULT FALSE,
  survey_male BOOLEAN,
  survey_female BOOLEAN,
  survey_min_age INT,
  survey_max_age INT,
  survey_country VARCHAR(80),
  surveys_answered_week BIGINT NOT NULL DEFAULT 0,
  surveys_started_week BIGINT NOT NULL DEFAULT 0,
  right_callcenter BOOLEAN DEFAULT FALSE,
  right_marketing BOOLEAN DEFAULT FALSE,
  right_user_admin BOOLEAN DEFAULT FALSE,
  right_security BOOLEAN DEFAULT FALSE,
  PRIMARY KEY(id)
);

CREATE TABLE country (
  alpha3 VARCHAR(3) NOT NULL,
  name_eng VARCHAR(50) NOT NULL,
  active BOOLEAN DEFAULT FALSE,
  PRIMARY KEY(alpha3)
);

CREATE TABLE survey (
  id SERIAL,
  user_id BIGINT NOT NULL,
  type VARCHAR(25) NOT NULL,
  status VARCHAR(25) NOT NULL,
  started_date TIMESTAMPTZ,
  max_answers INT NOT NULL,
  max_abuse INT NOT NULL,
  title VARCHAR(25),
  pic1 VARCHAR NOT NULL,
  pic2 VARCHAR NOT NULL,
  min_age INT NOT NULL DEFAULT 1,
  max_age INT NOT NULL DEFAULT 99,
  countries VARCHAR(80) NOT NULL ,
  male BOOLEAN NOT NULL,
  female BOOLEAN NOT NULL,
  answered INT NOT NULL DEFAULT 0,
  no_opinion_count INT NOT NULL DEFAULT 0,
  pic1count INT NOT NULL DEFAULT 0,
  pic2count INT NOT NULL DEFAULT 0,
  abuse_count INT NOT NULL DEFAULT 0,
  expected_answer SMALLINT,
  PRIMARY KEY(id)
);

CREATE TABLE answer (
  id SERIAL,
  user_id BIGINT NOT NULL,
  survey_id BIGINT NOT NULL,
  answer_time TIMESTAMPTZ NOT NULL,
  answer INT NOT NULL,
  age INT NOT NULL,
  country VARCHAR(3) NOT NULL,
  male BOOLEAN NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE feedback (
  id SERIAL,
  user_id BIGINT NOT NULL,
  type VARCHAR(25) NOT NULL,
  status VARCHAR(25) NOT NULL,
  send_date TIMESTAMPTZ NOT NULL,
  last_action_date TIMESTAMPTZ NOT NULL,
  title VARCHAR(50) NOT NULL,
  message VARCHAR(255) NOT NULL,
  answers INT NOT NULL,
  unread_answers INT NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE feedback_answer (
  id SERIAL,
  feedback_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  admin_id BIGINT,
  read_answer BOOLEAN NOT NULL,
  send_date TIMESTAMPTZ NOT NULL,
  message VARCHAR(255) NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE announcement (
  id SERIAL,
  admin_id BIGINT NOT NULL,
  send_date TIMESTAMPTZ NOT NULL,
  countries VARCHAR(255) NOT NULL ,
  title VARCHAR(50) NOT NULL,
  message TEXT NOT NULL,
  PRIMARY KEY(id)
);

