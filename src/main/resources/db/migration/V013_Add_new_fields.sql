ALTER TABLE envelopes
  ADD COLUMN caseNumber VARCHAR(100) NULL;

ALTER TABLE envelopes
  ADD COLUMN documentControlNumber VARCHAR(100);

ALTER TABLE payments
  ADD COLUMN paymentInstrumentNumber VARCHAR(100),
  ADD COLUMN sortCode VARCHAR(50) NULL,
  ADD COLUMN accountNumber VARCHAR(50) NULL;
