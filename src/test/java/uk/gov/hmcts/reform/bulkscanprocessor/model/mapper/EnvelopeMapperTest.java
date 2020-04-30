package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputNonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.getEnvelopeFromMetafile;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype.COVERSHEET;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype.SSCS1;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype.WILL;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.CHERISHED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.FORM;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.OTHER;

public class EnvelopeMapperTest {

    @Test
    public void should_map_zip_envelope_correctly() throws Exception {
        InputEnvelope zipEnvelope = getEnvelopeFromMetafile();
        String container = "container1";
        OcrValidationWarnings ocrValidationWarnings = new OcrValidationWarnings(
            zipEnvelope.scannableItems.get(0).documentControlNumber,
            asList("warning 1", "warning 2")
        );

        Envelope dbEnvelope = EnvelopeMapper.toDbEnvelope(zipEnvelope, container, Optional.of(ocrValidationWarnings));

        assertThat(dbEnvelope.getCaseNumber()).isEqualTo(zipEnvelope.caseNumber);
        assertThat(dbEnvelope.getPreviousServiceCaseReference()).isEqualTo(zipEnvelope.previousServiceCaseReference);
        assertThat(dbEnvelope.getPoBox()).isEqualTo(zipEnvelope.poBox);
        assertThat(dbEnvelope.getJurisdiction()).isEqualTo(zipEnvelope.jurisdiction);
        assertThat(dbEnvelope.getDeliveryDate()).isEqualTo(zipEnvelope.deliveryDate);
        assertThat(dbEnvelope.getOpeningDate()).isEqualTo(zipEnvelope.openingDate);
        assertThat(dbEnvelope.getZipFileCreateddate()).isEqualTo(zipEnvelope.zipFileCreateddate);
        assertThat(dbEnvelope.getZipFileName()).isEqualTo(zipEnvelope.zipFileName);
        assertThat(dbEnvelope.getClassification()).isEqualTo(zipEnvelope.classification);

        assertSamePayments(dbEnvelope, zipEnvelope);
        assertSameScannableItems(dbEnvelope, zipEnvelope);
        assertSameNonScannableItems(dbEnvelope, zipEnvelope);
        assertRightScannableItemsHaveWarnings(dbEnvelope, ocrValidationWarnings);

        // properties specific to DB envelope
        assertThat(dbEnvelope.getId()).isNull();
        assertThat(dbEnvelope.getStatus()).isEqualTo(Status.CREATED);
        assertThat(dbEnvelope.getUploadFailureCount()).isEqualTo(0);
        assertThat(dbEnvelope.isZipDeleted()).isFalse();
        assertThat(dbEnvelope.getCreatedAt()).isBefore(Instant.now().plusMillis(1));
        assertThat(dbEnvelope.getCreatedAt()).isAfter(Instant.now().minusSeconds(1));
        assertThat(dbEnvelope.getContainer()).isEqualTo(container);
    }

    private void assertSamePayments(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getPayments().size()).isEqualTo(zipEnvelope.payments.size());

        assertThat(dbEnvelope.getPayments())
            .extracting(Payment::getDocumentControlNumber)
            .containsAll(zipEnvelope.payments.stream().map(p -> p.documentControlNumber).collect(toList()));
    }

    private void assertSameScannableItems(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getScannableItems().size()).isEqualTo(zipEnvelope.scannableItems.size());

        assertThat(dbEnvelope.getScannableItems())
            .extracting(this::convertToInputScannableItem)
            .usingElementComparatorIgnoringFields("ocrData", "documentType")
            .containsAll(zipEnvelope.scannableItems);
        assertThat(dbEnvelope.getScannableItems())
            .extracting(ScannableItem::getOcrData)
            .extracting(this::convertToInputOcrData)
            .usingRecursiveFieldByFieldElementComparator()
            .containsAll(
                zipEnvelope.scannableItems.stream().map(item -> item.ocrData).collect(toList())
            );
        assertThat(dbEnvelope.getScannableItems())
            .extracting(ScannableItem::getDocumentType)
            .usingDefaultComparator()
            .containsExactly(CHERISHED, OTHER, FORM, DocumentType.COVERSHEET);
    }

    private void assertSameNonScannableItems(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getNonScannableItems().size())
            .isEqualTo(zipEnvelope.nonScannableItems.size());

        assertThat(dbEnvelope.getNonScannableItems())
            .extracting(this::convertToInputNonScannableItem)
            .usingFieldByFieldElementComparator()
            .containsAll(zipEnvelope.nonScannableItems);
    }

    private void assertRightScannableItemsHaveWarnings(
        Envelope dbEnvelope,
        OcrValidationWarnings ocrValidationWarnings
    ) {
        for (ScannableItem scannableItem : dbEnvelope.getScannableItems()) {
            if (scannableItem.getDocumentControlNumber().equals(ocrValidationWarnings.documentControlNumber)) {
                assertThat(scannableItem.getOcrValidationWarnings())
                    .hasSameElementsAs(ocrValidationWarnings.warnings);
            } else {
                assertThat(scannableItem.getOcrValidationWarnings()).isEmpty();
            }
        }
    }

    private InputScannableItem convertToInputScannableItem(ScannableItem dbScannableItem) {
        return new InputScannableItem(
            dbScannableItem.getDocumentControlNumber(),
            dbScannableItem.getScanningDate(),
            dbScannableItem.getOcrAccuracy(),
            dbScannableItem.getManualIntervention(),
            dbScannableItem.getNextAction(),
            dbScannableItem.getNextActionDate(),
            convertToInputOcrData(dbScannableItem.getOcrData()),
            dbScannableItem.getFileName(),
            dbScannableItem.getNotes(),
            convertToInputDocumentType(
                dbScannableItem.getDocumentType(),
                dbScannableItem.getDocumentSubtype()
            ),
            null
        );
    }

    private InputOcrData convertToInputOcrData(OcrData ocrData) {
        if (ocrData == null) {
            return null;
        }

        InputOcrData inputOcrData = new InputOcrData();
        List<InputOcrDataField> fields = ocrData
            .fields
            .stream()
            .map(field -> new InputOcrDataField(field.name, field.value))
            .collect(toList());
        inputOcrData.setFields(fields);

        return inputOcrData;
    }

    private InputDocumentType convertToInputDocumentType(
        DocumentType documentType,
        String documentSubtype
    ) {
        switch (documentType) {
            case CHERISHED:
                return InputDocumentType.CHERISHED;
            case OTHER:
                if (documentSubtype == null) {
                    return InputDocumentType.OTHER;
                } else {
                    switch (documentSubtype) {
                        case SSCS1:
                            return InputDocumentType.SSCS1;
                        case WILL:
                            return InputDocumentType.WILL;
                        case COVERSHEET:
                            return InputDocumentType.COVERSHEET;
                        default:
                            return InputDocumentType.OTHER;
                    }
                }
            case COVERSHEET:
                return InputDocumentType.COVERSHEET;
            case FORM:
                return InputDocumentType.FORM;
            default:
                throw new AssertionError(
                    String.format("Expected a valid document type but got: %s", documentType)
                );
        }
    }

    private InputNonScannableItem convertToInputNonScannableItem(NonScannableItem dbNonScannableItem) {
        return new InputNonScannableItem(
            dbNonScannableItem.getDocumentControlNumber(),
            dbNonScannableItem.getItemType(),
            dbNonScannableItem.getNotes()
        );
    }
}
