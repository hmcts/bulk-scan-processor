-- Extracts the text after the last slash (/) in the documentUrl
UPDATE scannable_items
 SET documentUuid = REVERSE(SPLIT_PART(REVERSE(documentUrl), '/', 1))
