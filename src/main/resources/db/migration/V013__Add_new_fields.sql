ALTER TABLE envelopes
  ADD COLUMN caseNumber VARCHAR(100) NULL;

ALTER TABLE non_scannable_items
  ADD COLUMN documentControlNumber VARCHAR(100) NULL;

ALTER TABLE payments
  ADD COLUMN paymentInstrumentNumber VARCHAR(100) NULL,
  ADD COLUMN sortCode VARCHAR(10) NULL,
  ADD COLUMN accountNumber VARCHAR(50) NULL;

UPDATE payments
  SET sortCode = '112233',
      accountNumber = '12345678'
  WHERE method = 'Cheque' AND sortCode IS NULL AND accountNumber IS NULL;
