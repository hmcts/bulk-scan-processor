UPDATE scannable_items
SET ocrdata = null
WHERE envelope_id IN
(
  SELECT id FROM envelopes
  WHERE status = 'NOTIFICATION_SENT' AND createdat < '2019-01-26'
);

INSERT INTO process_events (container, zipfilename, createdat, event)
SELECT container, zipFileName, now(), 'COMPLETED'
FROM envelopes
WHERE status = 'NOTIFICATION_SENT' AND createdat < '2019-01-26';

UPDATE envelopes
SET status = 'COMPLETED'
WHERE status = 'NOTIFICATION_SENT' AND createdat < '2019-01-26';
