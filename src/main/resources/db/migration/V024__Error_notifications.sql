CREATE TABLE error_notifications (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES process_events(id),
  notificationId VARCHAR(50) NULL,
  errorCode VARCHAR(20) NOT NULL,
  createdAt TIMESTAMP NOT NULL
);
