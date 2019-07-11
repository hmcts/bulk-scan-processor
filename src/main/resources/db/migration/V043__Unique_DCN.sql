ALTER TABLE scannable_items
  ADD CONSTRAINT scannable_item_dcn UNIQUE (documentControlNumber);
