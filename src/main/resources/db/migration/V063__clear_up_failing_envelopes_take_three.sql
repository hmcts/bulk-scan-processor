-- Remove envelopes that failed due to downstream issue, they will be picked up again on next cron
DELETE FROM scannable_items
WHERE envelope_id = '21a14d01-fd64-461c-a7e0-9af1007877e7';

DELETE FROM scannable_items
WHERE envelope_id = 'fd485f04-64f9-46f2-aad3-be362a8db4fb';

DELETE FROM scannable_items
WHERE envelope_id = '78425c2d-bc1b-4178-950d-0c794f6a7b08';

DELETE FROM envelopes
WHERE id = '21a14d01-fd64-461c-a7e0-9af1007877e7';

DELETE FROM envelopes
WHERE id = 'fd485f04-64f9-46f2-aad3-be362a8db4fb';

DELETE FROM envelopes
WHERE id = '78425c2d-bc1b-4178-950d-0c794f6a7b08';

DELETE FROM process_events
WHERE zipfilename IN (
  '2523004020819_19-08-2025-08-25-23.zip',
  '2522604011908_15-08-2025-05-38-26.zip',
  '2523904010957_27-08-2025-11-49-56.zip'
);

