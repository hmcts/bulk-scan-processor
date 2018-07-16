package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "envelope_state")
public class EnvelopeState implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String container;
    @JsonProperty("zip_file_name")
    private String zipFileName;
    @JsonProperty("created_at")
    private Timestamp createdAt = Timestamp.from(Instant.now());
    @Enumerated(EnumType.STRING)
    private EnvelopeStatus status;
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id")
    private Envelope envelope;

    private EnvelopeState() {
        // For use by hibernate.
    }

    public EnvelopeState(String containerName, String zipFileName, EnvelopeStatus status) {
        this.container = containerName;
        this.zipFileName = zipFileName;
        this.status = status;
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

    public UUID getId() {
        return id;
    }

    public EnvelopeStatus getStatus() {
        return status;
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
