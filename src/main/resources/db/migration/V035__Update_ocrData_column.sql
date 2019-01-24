ALTER TABLE scannable_items
  DROP COLUMN ocrData;

ALTER TABLE scannable_items
  ADD COLUMN ocrData JSON null

