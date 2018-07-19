DROP TABLE envelope_state;

ALTER TABLE envelopes
  ALTER COLUMN lastEvent DROP DEFAULT;
