-- Remove envelopes, as they have been processed manually by the relevant teams
DELETE FROM scannable_items
WHERE envelope_id = '7841483b-2221-4e0a-90c7-b3d51461b6cf';

DELETE FROM scannable_items
WHERE envelope_id = '4f3df786-0df1-41e1-b872-344a8824e823';

DELETE FROM scannable_items
WHERE envelope_id = 'c1450243-d8e7-48b9-a838-640a1b9d31ad';

DELETE FROM envelopes
WHERE id = '7841483b-2221-4e0a-90c7-b3d51461b6cf';

DELETE FROM envelopes
WHERE id = '4f3df786-0df1-41e1-b872-344a8824e823';

DELETE FROM envelopes
WHERE id = 'c1450243-d8e7-48b9-a838-640a1b9d31ad';

DELETE FROM process_events
WHERE zipfilename IN (
  '2519906010990_18-07-2025-13-24-07.zip',
  '2521704010235_05-08-2025-11-32-59.zip',
  '2520206011178_21-07-2025-11-30-50.zip'
);
