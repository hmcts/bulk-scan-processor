-- Remove envelopes that failed due to downstream issue, they will be picked up again on next cron
DELETE FROM scannable_items
WHERE envelope_id = '1667508f-db71-4125-9406-b904efe7234b';

DELETE FROM envelopes
WHERE id = '1667508f-db71-4125-9406-b904efe7234b';

DELETE FROM process_events
WHERE zipfilename IN (
  '2614006010591_20-05-2026-07-14-47.zip'
);
