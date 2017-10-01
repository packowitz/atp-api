ALTER TABLE "notification" ADD atp_answerable_send_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE "notification" ADD atp_answerable_between_time TIME DEFAULT make_time(23,59,59) NOT NULL;