ALTER TABLE "user" ADD COLUMN age_range INTEGER;

UPDATE "user" SET age_range = 1 WHERE yearofbirth > 2007;
UPDATE "user" SET age_range = 2 WHERE yearofbirth > 2004 AND yearofbirth <= 2007;
UPDATE "user" SET age_range = 3 WHERE yearofbirth > 2001 AND yearofbirth <= 2004;
UPDATE "user" SET age_range = 4 WHERE yearofbirth > 1999 AND yearofbirth <= 2001;
UPDATE "user" SET age_range = 5 WHERE yearofbirth > 1996 AND yearofbirth <= 1999;
UPDATE "user" SET age_range = 6 WHERE yearofbirth > 1987 AND yearofbirth <= 1996;
UPDATE "user" SET age_range = 7 WHERE yearofbirth > 1977 AND yearofbirth <= 1987;
UPDATE "user" SET age_range = 8 WHERE yearofbirth > 1961 AND yearofbirth <= 1977;
UPDATE "user" SET age_range = 9 WHERE yearofbirth <= 1961;


ALTER TABLE "survey" ADD COLUMN age_1 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_2 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_3 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_4 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_5 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_6 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_7 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_8 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "survey" ADD COLUMN age_9 BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE "survey" SET age_1 = TRUE where min_age <= 9;
UPDATE "survey" SET age_2 = TRUE where min_age <= 12 AND max_age >= 10;
UPDATE "survey" SET age_3 = TRUE where min_age <= 15 AND max_age >= 13;
UPDATE "survey" SET age_4 = TRUE where min_age <= 17 AND max_age >= 16;
UPDATE "survey" SET age_5 = TRUE where min_age <= 21 AND max_age >= 18;
UPDATE "survey" SET age_6 = TRUE where min_age <= 29 AND max_age >= 22;
UPDATE "survey" SET age_7 = TRUE where min_age <= 39 AND max_age >= 30;
UPDATE "survey" SET age_8 = TRUE where min_age <= 55 AND max_age >= 40;
UPDATE "survey" SET age_9 = TRUE where max_age >= 56;


ALTER TABLE "answer" ADD COLUMN age_range INTEGER;

UPDATE "answer" SET age_range = 1 WHERE age <= 9;
UPDATE "answer" SET age_range = 2 WHERE age >= 10 AND age <= 12;
UPDATE "answer" SET age_range = 3 WHERE age >= 13 AND age <= 15;
UPDATE "answer" SET age_range = 4 WHERE age >= 16 AND age <= 17;
UPDATE "answer" SET age_range = 5 WHERE age >= 18 AND age <= 21;
UPDATE "answer" SET age_range = 6 WHERE age >= 22 AND age <= 29;
UPDATE "answer" SET age_range = 7 WHERE age >= 30 AND age <= 39;
UPDATE "answer" SET age_range = 8 WHERE age >= 40 AND age <= 55;
UPDATE "answer" SET age_range = 9 WHERE age >= 56;

ALTER TABLE "answer" ALTER COLUMN age_range SET NOT NULL;