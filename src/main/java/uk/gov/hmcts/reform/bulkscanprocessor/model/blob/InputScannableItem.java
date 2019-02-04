package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.util.OcrDataDeserializer;

import java.time.Instant;

public class InputScannableItem {

    public final String documentControlNumber;
    public final Instant scanningDate;
    public final String ocrAccuracy;
    public final String manualIntervention;
    public final String nextAction;
    public final Instant nextActionDate;
    public final OcrData ocrData;
    public final String fileName;
    public final String notes;
    public final InputDocumentType documentType;

    @JsonCreator
    public InputScannableItem(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("scanning_date") Instant scanningDate,
        @JsonProperty("ocr_accuracy") String ocrAccuracy,
        @JsonProperty("manual_intervention") String manualIntervention,
        @JsonProperty("next_action") String nextAction,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("next_action_date") Instant nextActionDate,
        @JsonDeserialize(using = OcrDataDeserializer.class)
        @JsonProperty("ocr_data") OcrData ocrData,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("notes") String notes,
        @JsonProperty("document_type") InputDocumentType documentType
    ) {
        this.documentControlNumber = documentControlNumber;
        this.scanningDate = scanningDate;
        this.ocrAccuracy = ocrAccuracy;
        this.manualIntervention = manualIntervention;
        this.nextAction = nextAction;
        this.nextActionDate = nextActionDate;
        this.ocrData = ocrData;
        this.fileName = fileName;
        this.notes = notes;
        this.documentType = documentType;
    }
}
