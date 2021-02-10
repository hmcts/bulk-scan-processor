CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE envelopes
  ADD COLUMN lastmodified TIMESTAMPTZ NULL;

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON envelopes
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
