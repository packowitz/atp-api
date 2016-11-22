ALTER TABLE survey ADD COLUMN updated_date TIMESTAMPTZ DEFAULT 'now'::timestamptz;

CREATE FUNCTION auto_updated_date() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_date = NOW();
  RETURN NEW;
END;
$$;

CREATE TRIGGER survey_updated_date BEFORE UPDATE ON survey FOR EACH ROW EXECUTE PROCEDURE auto_updated_date();
