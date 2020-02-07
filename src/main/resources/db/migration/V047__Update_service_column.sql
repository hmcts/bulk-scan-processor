UPDATE error_notifications
  SET service = 'bulk_scan_processor'
  WHERE service IS NULL;
