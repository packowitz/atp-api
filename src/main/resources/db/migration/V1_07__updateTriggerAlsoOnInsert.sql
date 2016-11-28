DROP TRIGGER survey_updated_date ON survey;
CREATE TRIGGER survey_updated_date BEFORE INSERT OR UPDATE ON survey FOR EACH ROW EXECUTE PROCEDURE auto_updated_date();