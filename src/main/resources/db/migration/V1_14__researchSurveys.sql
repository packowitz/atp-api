ALTER TABLE user_rights ADD COLUMN research BOOLEAN DEFAULT FALSE;

UPDATE user_rights SET research = TRUE where user_id = 1;

ALTER TABLE survey ADD COLUMN days_between INT;