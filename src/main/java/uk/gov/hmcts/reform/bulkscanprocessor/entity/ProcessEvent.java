package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "process_events")
public class ProcessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String container;

    private String zipFileName;

    private Instant createdAt = Instant.now();

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
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
