ALTER TABLE scannable_items
ADD COLUMN documentSubtype VARCHAR(100) NULL;

UPDATE scannable_items
SET documentSubtype = 'sscs1' WHERE lower(documentType) = 'sscs1';

UPDATE scannable_items
SET documentType = 'other' WHERE lower(documentType) = 'sscs1';
