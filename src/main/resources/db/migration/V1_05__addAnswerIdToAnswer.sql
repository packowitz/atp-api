ALTER TABLE answer ADD COLUMN answer_id INTEGER;

UPDATE answer SET answer_id = answer;

ALTER TABLE answer ALTER COLUMN answer_id SET NOT NULL;

UPDATE answer SET answer = 1 WHERE answer_id = pic1_id;
UPDATE answer SET answer = 2 WHERE answer_id = pic2_id;