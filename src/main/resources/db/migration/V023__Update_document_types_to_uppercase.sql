UPDATE scannable_items
SET documentSubtype = upper(documentSubtype) where documentSubtype IS NOT NULL;

UPDATE scannable_items
SET documentType = upper(documentType);
