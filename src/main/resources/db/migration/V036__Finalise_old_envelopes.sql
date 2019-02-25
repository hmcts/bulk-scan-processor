-- This script finalises all processed envelopes created before 25th January 2019.
-- All newer envelopes will be finalised by the service itself.
-- In production no envelopes were created or processed between 19th and 28th Jan,
-- so 25th Jan is a safe date. On master, the change was introduced on 24th Jan,
-- but the script prevents duplicating events.

UPDATE scannable_items
SET ocrdata = null
WHERE envelope_id IN
(
  SELECT id FROM envelopes
  WHERE status = 'NOTIFICATION_SENT' AND createdat < '2019-01-25'
);

INSERT INTO process_events (container, zipfilename, createdat, event)
SELECT container, zipfilename, now(), 'COMPLETED'
FROM envelopes
WHERE status = 'NOTIFICATION_SENT'
  AND createdat < '2019-01-25'
  AND (container, zipfilename) NOT IN (
    SELECT container, zipfilename FROM process_events
    WHERE event = 'COMPLETED'
  );

UPDATE envelopes
SET status = 'COMPLETED'
WHERE status = 'NOTIFICATION_SENT' AND createdat < '2019-01-25';
