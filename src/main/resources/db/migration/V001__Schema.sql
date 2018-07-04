CREATE TABLE envelopes (
  id UUID PRIMARY KEY,
  poBox VARCHAR(50) NOT NULL,
  jurisdiction VARCHAR(50) NOT NULL,
  deliveryDate TIMESTAMP NOT NULL,
  openingDate TIMESTAMP NOT NULL,
  zipFileCreatedDate TIMESTAMP NOT NULL,
  zipFileName VARCHAR(255) NOT NULL
);

CREATE TABLE scannable_items (
  id UUID PRIMARY KEY,
  documentControlNumber VARCHAR(100),
  scanningDate TIMESTAMP,
  ocrAccuracy VARCHAR(50) NULL,
  manualIntervention VARCHAR(255) NULL,
  nextAction VARCHAR(50),
  nextActionDate TIMESTAMP,
  ocrData TEXT NULL,
  fileName VARCHAR(255),
  notes TEXT,
  envelope_id UUID REFERENCES envelopes(id)
);

CREATE TABLE payments (
  id UUID PRIMARY KEY,
  documentControlNumber VARCHAR(100),
  method VARCHAR(50),
  amountInPence INTEGER,
  currency VARCHAR(3),
  envelope_id UUID REFERENCES envelopes(id)
);

CREATE TABLE non_scannable_items (
  id UUID PRIMARY KEY,
  itemType VARCHAR(50),
  notes TEXT,
  envelope_id UUID REFERENCES envelopes(id)
);
