package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Collections.emptyList;

public class Envelope {

    public static final String TEST_FILE_SUFFIX = ".test.zip";

    public final String caseNumber;
    public final String poBox;
    public final String jurisdiction;
    public final Timestamp deliveryDate;
    public final Timestamp openingDate;
    public final Timestamp zipFileCreateddate;
    public final String zipFileName;
    public final Classification classification;
    public final List<ScannableItem> scannableItems;
    public final List<Payment> payments;
    public final List<NonScannableItem> nonScannableItems;

    @JsonCreator
    public Envelope(
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("delivery_date") Timestamp deliveryDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("opening_date") Timestamp openingDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("zip_file_createddate") Timestamp zipFileCreateddate,
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("case_number") String caseNumber,
        @JsonProperty("classification") Classification classification,
        @JsonProperty("scannable_items") List<ScannableItem> scannableItems,
        @JsonProperty("payments") List<Payment> payments,
        @JsonProperty("non_scannable_items") List<NonScannableItem> nonScannableItems
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.caseNumber = caseNumber;
        this.classification = classification;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
    }

    //    TODO: delete?
    public boolean isTestOnly() {
        return !Strings.isNullOrEmpty(zipFileName)
            && zipFileName.toLowerCase().endsWith(TEST_FILE_SUFFIX);
    }
}
