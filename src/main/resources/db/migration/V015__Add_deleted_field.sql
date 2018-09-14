ALTER TABLE envelopes
  ADD COLUMN zipDeleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE envelopes
  SET zipDeleted = TRUE;

UPDATE envelopes
  SET zipDeleted = FALSE,
      status = 'PROCESSED'
  WHERE status = 'DELETE_BLOB_FAILURE';
