package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Table(name = "envelope_statuses")
public class EnvelopeStatus implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String container;
    @JsonProperty("zip_file_name")
    private String zipFileName;
    @Enumerated(EnumType.STRING)
    private EnvelopeStatusEnum status;
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id")
    private Envelope envelope;

    private EnvelopeStatus() {
        // For use by hibernate.
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
