CREATE TABLE error_notifications (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES process_events(id),
  notificationId VARCHAR(50) NULL,
  documentControlNumber VARCHAR(100) NULL,
  errorCode VARCHAR(20) NOT NULL,
  errorDescription TEXT NOT NULL,
  referenceId VARCHAR(50) NULL,
  createdAt TIMESTAMP NOT NULL
);
