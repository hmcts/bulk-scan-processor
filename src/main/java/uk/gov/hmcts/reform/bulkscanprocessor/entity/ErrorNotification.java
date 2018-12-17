package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "error_notifications")
public class ErrorNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long eventId;

    private String notificationId;

    @SuppressWarnings("squid:S1068") // unused field
    private String zipFileName;

    @SuppressWarnings("squid:S1068") // unused field
    private String documentControlNumber;

    private String errorCode;

    private String errorDescription;

    @SuppressWarnings("squid:S1068") // unused field
    private String referenceId;

    private Timestamp createdAt = Timestamp.from(Instant.now());

    private ErrorNotification() {
        // For use by hibernate.
    }

    public ErrorNotification(
        long eventId,
        String notificationId,
        String zipFileName,
        String errorCode,
        String errorDescription
    ) {
        this.eventId = eventId;
        this.notificationId = notificationId;
        this.zipFileName = zipFileName;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public long getId() {
        return id;
    }

    public long getEventId() {
        return eventId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
