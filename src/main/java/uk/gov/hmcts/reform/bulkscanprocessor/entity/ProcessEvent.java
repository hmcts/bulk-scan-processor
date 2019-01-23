package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "process_events")
public class ProcessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String container;

    private String zipFileName;

    private Timestamp createdAt = Timestamp.from(Instant.now());

    @Enumerated(EnumType.STRING)
    private Event event;
    private String reason;

    public ProcessEvent() {
        // For use by hibernate.
    }

    public ProcessEvent(String containerName, String zipFileName, Event event) {
        this.container = containerName;
        this.zipFileName = zipFileName;
        this.event = event;
    }

    public String getContainer() {
        return container;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
