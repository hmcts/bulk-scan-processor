-- Remove envelopes that failed due to downstream issue, they will be picked up again on next cron
DELETE FROM scannable_items
WHERE envelope_id = '86eeafe0-fb57-41fc-8e13-90f9fba2deb1';

DELETE FROM scannable_items
WHERE envelope_id = '14734ade-3872-4cd0-952c-46a8b86a3ad5';

DELETE FROM scannable_items
WHERE envelope_id = '15de94c4-975b-4211-9e7f-d2c963461353';

DELETE FROM scannable_items
WHERE envelope_id = 'dfc4d570-7d9d-45bb-bb96-ef13b2f56477';

DELETE FROM envelopes
WHERE id = '86eeafe0-fb57-41fc-8e13-90f9fba2deb1';

DELETE FROM envelopes
WHERE id = '14734ade-3872-4cd0-952c-46a8b86a3ad5';

DELETE FROM envelopes
WHERE id = '15de94c4-975b-4211-9e7f-d2c963461353';

DELETE FROM envelopes
WHERE id = 'dfc4d570-7d9d-45bb-bb96-ef13b2f56477';

DELETE FROM process_events
WHERE zipfilename IN (
  '2524804011420_05-09-2025-11-28-29.zip',
  '2523004020819_19-08-2025-08-25-23.zip',
  '2523904010957_27-08-2025-11-49-56.zip',
  '2522604011908_15-08-2025-05-38-26.zip'
);
