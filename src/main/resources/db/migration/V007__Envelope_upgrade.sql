ALTER TABLE process_events DROP COLUMN envelope_id;

ALTER TABLE envelopes
  ADD COLUMN container VARCHAR(50) NULL,
  ADD CONSTRAINT envelopes_container_zipFileName UNIQUE (container, zipFileName);

ALTER TABLE envelopes
  RENAME COLUMN lastEvent TO status;
