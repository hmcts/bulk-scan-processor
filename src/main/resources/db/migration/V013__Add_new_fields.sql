ALTER TABLE envelopes
  ADD COLUMN caseNumber VARCHAR(100) NULL;

ALTER TABLE non_scannable_items
  ADD COLUMN documentControlNumber VARCHAR(100) NOT NULL;

ALTER TABLE payments
  ADD COLUMN paymentInstrumentNumber VARCHAR(100),
  ADD COLUMN sortCode VARCHAR(50) NULL,
  ADD COLUMN accountNumber VARCHAR(50) NULL;
