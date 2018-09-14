ALTER TABLE envelopes
  ADD COLUMN caseNumber VARCHAR(100) NULL;

ALTER TABLE non_scannable_items
  ADD COLUMN documentControlNumber VARCHAR(100) NOT NULL DEFAULT '1111111';

ALTER TABLE payments
  ADD COLUMN paymentInstrumentNumber VARCHAR(100) NULL,
  ADD COLUMN sortCode VARCHAR(10) NULL,
  ADD COLUMN accountNumber VARCHAR(50) NULL;

UPDATE payments
  SET sortCode = '111111',
      accountNumber = '11111111'
  WHERE method = 'Cheque' AND sortCode IS NULL AND accountNumber IS NULL;
