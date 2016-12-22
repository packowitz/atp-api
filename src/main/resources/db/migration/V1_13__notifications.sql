ALTER TABLE public."user" DROP notifications;
ALTER TABLE public."user" DROP notifications_sound;
ALTER TABLE public."user" DROP notifications_vibration;
ALTER TABLE public."user" DROP notification_reg_id;
ALTER TABLE public."user" DROP device_os;

CREATE TABLE notification (
  user_id BIGINT NOT NULL,
  device_id VARCHAR(255) NOT NULL,
  os VARCHAR(25),
  token TEXT,
  atp_answerable_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  atp_finished_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  announcement_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  feedback_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY(user_id, device_id)
);