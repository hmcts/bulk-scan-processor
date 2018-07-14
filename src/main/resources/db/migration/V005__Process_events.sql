CREATE TABLE process_events (
  id BIGSERIAL PRIMARY KEY,
  container VARCHAR(50) NOT NULL,
  zipFileName VARCHAR(255) NOT NULL,
  createdAt TIMESTAMP NOT NULL,
  envelope_id VARCHAR(50) NULL,
  event VARCHAR(100) NOT NULL,
  reason TEXT NULL
);

CREATE INDEX process_events_container_zip_idx ON process_events (container, zipFileName);
CREATE INDEX process_events_container_event_idx ON process_events (container, event);
CREATE INDEX process_events_zip_idx ON process_events (zipFileName);

ALTER TABLE envelopes
  ADD COLUMN lastEvent VARCHAR(100) NOT NULL DEFAULT 'ENVELOPE_CREATED';

UPDATE envelopes e SET lastEvent = 'DOC_UPLOADED' WHERE id = (
  SELECT envelope_id FROM scannable_items si WHERE si.documentUrl IS NOT NULL GROUP BY envelope_id
);
