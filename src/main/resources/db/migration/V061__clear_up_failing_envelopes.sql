-- Remove envelopes, as they have been processed manually by the relevant teams
DELETE FROM scannable_items
WHERE envelope_id = '9788ab63-8f8f-4c10-9c82-ae82e43a7f34';

DELETE FROM scannable_items
WHERE envelope_id = 'de118c8e-ac70-47da-bd1c-7307ea23d95c';

DELETE FROM scannable_items
WHERE envelope_id = '1abfb6f3-f306-40ba-ad0a-5caa27eb293b';

DELETE FROM envelopes
WHERE id = '9788ab63-8f8f-4c10-9c82-ae82e43a7f34';

DELETE FROM envelopes
WHERE id = 'de118c8e-ac70-47da-bd1c-7307ea23d95c';

DELETE FROM envelopes
WHERE id = '1abfb6f3-f306-40ba-ad0a-5caa27eb293b';

DELETE FROM process_events
WHERE zipfilename IN (
  '2519906010990_18-07-2025-13-24-07.zip',
  '2521704010235_05-08-2025-11-32-59.zip',
  '2520206011178_21-07-2025-11-30-50.zip'
);
