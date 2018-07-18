package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "process_events")
public class ProcessEvent implements EnvelopeAssignable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String container;
    @JsonProperty("zip_file_name")
    private String zipFileName;
    @JsonProperty("created_at")
    private Timestamp createdAt = Timestamp.from(Instant.now());
    @Enumerated(EnumType.STRING)
    private Event event;
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id")
    private Envelope envelope;

    private ProcessEvent() {
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

    public Envelope getEnvelope() {
        return envelope;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
