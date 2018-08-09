ALTER TABLE envelopes
  ADD COLUMN classification VARCHAR(50) NULL,
  ADD COLUMN urgent BOOLEAN NULL;

ALTER TABLE scannable_items
  ADD COLUMN documentType VARCHAR(100) NULL;
