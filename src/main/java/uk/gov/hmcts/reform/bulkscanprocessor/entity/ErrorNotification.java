package uk.gov.hmcts.reform.bulkscanprocessor.entity;

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

    private String service;

    private final Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private ProcessEvent event;

    private ErrorNotification() {
        // For use by hibernate.
    }

    public ErrorNotification(ProcessEvent event, String errorCode, String service) {
        this.event = event;
        this.errorCode = errorCode;
        this.service = service;
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

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getService() {
        return service;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
