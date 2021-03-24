ALTER TABLE payments
  ADD COLUMN lastmodified TIMESTAMP NULL,
  ADD COLUMN status VARCHAR(100) NULL;


CREATE TRIGGER set_timestamp
    BEFORE UPDATE ON payments
    FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
