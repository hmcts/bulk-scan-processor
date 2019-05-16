UPDATE scannable_items
 SET documentUrl = REVERSE(SPLIT_PART(REVERSE(documentUrl), '/', 1))
