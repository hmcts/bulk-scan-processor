ALTER TABLE envelopes
  ADD COLUMN lastmodified TIMESTAMPTZ NULL;

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.lastmodified = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON envelopes
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
