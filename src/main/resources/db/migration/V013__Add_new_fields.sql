ALTER TABLE envelopes
  ADD COLUMN caseNumber VARCHAR(100) NULL;

ALTER TABLE non_scannable_items
  ADD COLUMN documentControlNumber VARCHAR(100) NOT NULL;

ALTER TABLE payments
  ADD COLUMN paymentInstrumentNumber VARCHAR(100),
  ADD COLUMN sortCode VARCHAR(10) NULL DEFAULT 112233,
  ADD COLUMN accountNumber VARCHAR(50) NULL DEFAULT 12345678;

ALTER TABLE scannable_items
  ALTER COLUMN documentControlNumber SET NOT NULL;
