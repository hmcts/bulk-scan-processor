-- This script creates ZIPFILE_PROCESSING_STARTED events for those files that
-- don't have them, yet, but have other events. This helps create
-- reports for dates when this event wasn't created by the service.

INSERT INTO process_events (container, zipfilename, createdat, event, reason)
SELECT container, zipfilename, min(createdat), 'ZIPFILE_PROCESSING_STARTED', 'Created by migration script'
FROM process_events
WHERE (container, zipfilename) NOT IN (
  SELECT container, zipfilename FROM process_events
  WHERE event = 'ZIPFILE_PROCESSING_STARTED'
)
GROUP BY container, zipfilename;
