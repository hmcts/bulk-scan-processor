package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;
import java.util.UUID;

public class PaymentResponse {

    public final UUID id;

    public final String status;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("last_modified")
    public Instant lastmodified;

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    public PaymentResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("status") String status,
        @JsonProperty("last_modified") Instant lastmodified) {
        this.id = id;
        this.documentControlNumber = documentControlNumber;
        this.status = status;
        this.lastmodified = lastmodified;
    }

}
