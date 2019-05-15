ALTER TABLE scannable_items
  ADD COLUMN documentUuid VARCHAR(100) NULL;

UPDATE scannable_items
   SET documentUuid = (
     SELECT documentId[1] FROM
        REGEXP_MATCHES(
        documenturl,
        '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}',
        'g'
        ) AS documentId
     );
