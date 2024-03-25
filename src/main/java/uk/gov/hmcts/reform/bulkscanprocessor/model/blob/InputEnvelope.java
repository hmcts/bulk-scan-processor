package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantDeserializer;

import java.time.Instant;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Represents an envelope that is sent to the system.
 */
public class InputEnvelope {

    public final String caseNumber;
    public final String previousServiceCaseReference;
    public final String poBox;
    public final String jurisdiction;
    public final Instant deliveryDate;
    public final Instant openingDate;
    public final Instant zipFileCreateddate;
    public final String zipFileName;
    public final String rescanFor;
    public final Classification classification;
    public final List<InputScannableItem> scannableItems;
    public final List<InputPayment> payments;
    public final List<InputNonScannableItem> nonScannableItems;

    /**
     * Constructor for input envelope.
     * @param poBox The PO box
     * @param jurisdiction The jurisdiction
     * @param deliveryDate The delivery date
     * @param openingDate The opening date
     * @param zipFileCreateddate The zip file created date
     * @param zipFileName The zip file name
     * @param rescanFor The rescan for
     * @param caseNumber The case number
     * @param previousServiceCaseReference The previous service case reference
     * @param classification The classification
     * @param scannableItems The scannable items
     * @param payments The payments
     * @param nonScannableItems The non scannable items
     */
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
        @JsonProperty("rescan_for") String rescanFor,
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
        this.rescanFor = rescanFor;
        // scanning can add spaces
        this.caseNumber = StringUtils.trim(caseNumber);
        this.previousServiceCaseReference = previousServiceCaseReference;
        this.classification = classification;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
    }
}
