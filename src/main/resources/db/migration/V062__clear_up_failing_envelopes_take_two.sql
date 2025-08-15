-- Remove envelopes, as they have been processed manually by the relevant teams
DELETE FROM scannable_items
WHERE envelope_id = 'c30a21f8-01a4-46d7-aedb-aa9dd5278dc8';

DELETE FROM scannable_items
WHERE envelope_id = 'fcc8867a-db1c-457a-8833-1bc8af27c575';

DELETE FROM scannable_items
WHERE envelope_id = 'dec8ee53-daf1-4051-89fb-c98c8025465c';

DELETE FROM envelopes
WHERE id = 'c30a21f8-01a4-46d7-aedb-aa9dd5278dc8';

DELETE FROM envelopes
WHERE id = 'fcc8867a-db1c-457a-8833-1bc8af27c575';

DELETE FROM envelopes
WHERE id = 'dec8ee53-daf1-4051-89fb-c98c8025465c';

DELETE FROM process_events
WHERE zipfilename IN (
  '2519906010990_18-07-2025-13-24-07.zip',
  '2521704010235_05-08-2025-11-32-59.zip',
  '2520206011178_21-07-2025-11-30-50.zip'
);
