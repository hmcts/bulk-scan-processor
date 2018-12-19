CREATE TABLE error_notifications (
  id BIGSERIAL PRIMARY KEY,
  eventId BIGINT NOT NULL,
  notificationId VARCHAR(50) NULL,
  zipFileName VARCHAR(255) NOT NULL,
  documentControlNumber VARCHAR(100) NULL,
  errorCode VARCHAR(20) NOT NULL,
  errorDescription TEXT NOT NULL,
  referenceId VARCHAR(50) NULL,
  createdAt TIMESTAMP NOT NULL
);
