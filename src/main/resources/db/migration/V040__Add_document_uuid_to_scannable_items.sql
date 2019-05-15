ALTER TABLE scannable_items
  ADD COLUMN documentUuid VARCHAR(100) NULL;

-- Extract the text after the last slash (/) in the documentUrl
UPDATE scannable_items
   SET documentUuid = (
     SELECT documentId[1]
     FROM
     REGEXP_MATCHES(documentUrl, '([^/]+)(?=[^/]*/?$)', 'g') AS documentId);
