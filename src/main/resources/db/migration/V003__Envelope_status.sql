CREATE TABLE envelope_statuses (
  id UUID PRIMARY KEY,
  container VARCHAR(50) NOT NULL,
  zipFileName VARCHAR(255) NOT NULL,
  envelope_id VARCHAR(50) NULL,
  status VARCHAR(100) NOT NULL,
  reason TEXT NULL
);

CREATE INDEX envelope_statuses_container_zip_idx ON envelope_statuses (container, zipFileName);
CREATE INDEX envelope_statuses_container_status_idx ON envelope_statuses (container, status);
CREATE INDEX envelope_statuses_zip_idx ON envelope_statuses (zipFileName);
