ALTER TABLE scannable_items
  ADD COLUMN documentUuid VARCHAR(100) NULL;

UPDATE scannable_items
   SET documentUuid = (
     SELECT documentId[1] FROM
        REGEXP_MATCHES(
        documenturl,
        '([^/]+)(?=[^/]*/?$)',
        'g'
        ) AS documentId
     );
