package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "error_notifications")
public class ErrorNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String notificationId;

    private String errorCode;

    private Timestamp createdAt = Timestamp.from(Instant.now());

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private ProcessEvent event;

    private ErrorNotification() {
        // For use by hibernate.
    }

    public ErrorNotification(
        ProcessEvent event,
        String errorCode
    ) {
        this.event = event;
        this.errorCode = errorCode;
    }

    public long getId() {
        return id;
    }

    public ProcessEvent getProcessEvent() {
        return event;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
