package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.util.OcrDataDeserializer;

import java.time.Instant;

/**
 * Represents a scannable item in a document.
 */
public class InputScannableItem {

    public final String documentControlNumber;
    public final Instant scanningDate;
    public final String ocrAccuracy;
    public final String manualIntervention;
    public final String nextAction;
    public final Instant nextActionDate;
    public final InputOcrData ocrData;
    public final String fileName;
    public final String notes;
    public final InputDocumentType documentType;
    public final String documentSubtype;

    /**
     * Constructor for InputScannableItem.
     * @param documentControlNumber the document control number
     * @param scanningDate the date when the document was scanned
     * @param ocrAccuracy the accuracy of OCR
     * @param manualIntervention whether manual intervention is required
     * @param nextAction the next action
     * @param nextActionDate the date when the next action is due
     * @param ocrData the OCR data
     * @param fileName the name of the file
     * @param notes the notes
     * @param documentType the type of the document
     * @param documentSubtype the subtype of the document
     */
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
        @JsonProperty("ocr_data") InputOcrData ocrData,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("notes") String notes,
        @JsonProperty("document_type") InputDocumentType documentType,
        @JsonProperty("document_sub_type") String documentSubtype

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
        this.documentSubtype = documentSubtype;
    }
}
