package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents an event that happened to a zip file.
 */
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

    /**
     * Constructor.
     */
    public ProcessEvent() {
        // For use by hibernate.
    }

    /**
     * Constructor for process event.
     * @param containerName name of the container
     * @param zipFileName name of the zip file
     * @param event event that happened
     */
    public ProcessEvent(String containerName, String zipFileName, Event event) {
        this.container = containerName;
        this.zipFileName = zipFileName;
        this.event = event;
    }

    /**
     * Get the name of the container.
     * @return the name of the container
     * */
    public String getContainer() {
        return container;
    }

    /**
     * Get the name of the zip file.
     * @return the name of the zip file
     * */
    public String getZipFileName() {
        return zipFileName;
    }

    /**
     * Get the time the event was created.
     * @return the time the event was created
     * */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the time the event was created.
     * @param createdAt the time the event was created
     * */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the id of the event.
     * @return the id of the event
     * */
    public long getId() {
        return id;
    }

    /**
     * Get the event that happened.
     * @return the event that happened
     * */
    public Event getEvent() {
        return event;
    }

    /**
     * Get the reason for the event.
     * */
    public String getReason() {
        return reason;
    }

    /**
     * Set the reason for the event.
     * @param reason the reason for the event
     * */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
