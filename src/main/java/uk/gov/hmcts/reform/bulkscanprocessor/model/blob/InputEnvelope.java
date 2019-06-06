package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantDeserializer;

import java.time.Instant;
import java.util.List;

import static java.util.Collections.emptyList;

public class InputEnvelope {

    public final String caseNumber;
    public final String previousServiceCaseReference;
    public final String poBox;
    public final String jurisdiction;
    public final Instant deliveryDate;
    public final Instant openingDate;
    public final Instant zipFileCreateddate;
    public final String zipFileName;
    public final Classification classification;
    public final List<InputScannableItem> scannableItems;
    public final List<InputPayment> payments;
    public final List<InputNonScannableItem> nonScannableItems;

    @JsonCreator
    public InputEnvelope(
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("delivery_date") Instant deliveryDate,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("opening_date") Instant openingDate,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("zip_file_createddate") Instant zipFileCreateddate,
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("case_number") String caseNumber,
        @JsonProperty("previous_service_case_reference") String previousServiceCaseReference,
        @JsonProperty("envelope_classification") Classification classification,
        @JsonProperty("scannable_items") List<InputScannableItem> scannableItems,
        @JsonProperty("payments") List<InputPayment> payments,
        @JsonProperty("non_scannable_items") List<InputNonScannableItem> nonScannableItems
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.caseNumber = caseNumber;
        this.previousServiceCaseReference = previousServiceCaseReference;
        this.classification = classification;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
    }
}
