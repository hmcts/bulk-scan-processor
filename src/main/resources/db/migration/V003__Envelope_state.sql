CREATE TABLE envelope_state (
  id UUID PRIMARY KEY,
  container VARCHAR(50) NOT NULL,
  zipFileName VARCHAR(255) NOT NULL,
  createdAt TIMESTAMP NOT NULL,
  envelope_id VARCHAR(50) NULL,
  status VARCHAR(100) NOT NULL,
  reason TEXT NULL
);

CREATE INDEX envelope_state_container_zip_idx ON envelope_state (container, zipFileName);
CREATE INDEX envelope_state_container_status_idx ON envelope_state (container, status);
CREATE INDEX envelope_state_zip_idx ON envelope_state (zipFileName);
