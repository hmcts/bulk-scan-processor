package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.joda.time.DateTime;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputNonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.getEnvelopeFromMetafile;

public class EnvelopeMapperTest {

    @Test
    public void should_map_zip_envelope_correctly() throws Exception {
        InputEnvelope zipEnvelope = getEnvelopeFromMetafile();
        String container = "container1";

        Envelope dbEnvelope = EnvelopeMapper.toDbEnvelope(
            zipEnvelope,
            container
        );

        assertThat(dbEnvelope.getCaseNumber()).isEqualTo(zipEnvelope.caseNumber);
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

        // properties specific to DB envelope
        assertThat(dbEnvelope.getId()).isNull();
        assertThat(dbEnvelope.getStatus()).isEqualTo(Status.CREATED);
        assertThat(dbEnvelope.getUploadFailureCount()).isEqualTo(0);
        assertThat(dbEnvelope.isZipDeleted()).isFalse();
        assertThat(dbEnvelope.getCreatedAt()).isBefore(DateTime.now().plusMillis(1).toDate());
        assertThat(dbEnvelope.getCreatedAt()).isAfter(DateTime.now().minusSeconds(1).toDate());
        assertThat(dbEnvelope.getContainer()).isEqualTo(container);
    }

    private void assertSamePayments(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getPayments().size()).isEqualTo(zipEnvelope.payments.size());

        assertThat(dbEnvelope.getPayments())
            .extracting(this::convertToInputPayment)
            .usingFieldByFieldElementComparator()
            .containsAll(zipEnvelope.payments);
    }

    private void assertSameScannableItems(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getScannableItems().size()).isEqualTo(zipEnvelope.scannableItems.size());

        assertThat(dbEnvelope.getScannableItems())
            .extracting(this::convertToInputScannableItem)
            .usingFieldByFieldElementComparator()
            .containsAll(zipEnvelope.scannableItems);
    }

    private void assertSameNonScannableItems(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getNonScannableItems().size())
            .isEqualTo(zipEnvelope.nonScannableItems.size());

        assertThat(dbEnvelope.getNonScannableItems())
            .extracting(this::convertToInputNonScannableItem)
            .usingFieldByFieldElementComparator()
            .containsAll(zipEnvelope.nonScannableItems);
    }

    private InputPayment convertToInputPayment(Payment dbPayment) {
        return new InputPayment(
            dbPayment.getDocumentControlNumber(),
            dbPayment.getMethod(),
            String.format("%.2f", dbPayment.getAmount()),
            dbPayment.getCurrency(),
            dbPayment.getPaymentInstrumentNumber(),
            dbPayment.getSortCode(),
            dbPayment.getAccountNumber()
        );
    }

    private InputScannableItem convertToInputScannableItem(ScannableItem dbScannableItem) {
        return new InputScannableItem(
            dbScannableItem.getDocumentControlNumber(),
            dbScannableItem.getScanningDate(),
            dbScannableItem.getOcrAccuracy(),
            dbScannableItem.getManualIntervention(),
            dbScannableItem.getNextAction(),
            dbScannableItem.getNextActionDate(),
            dbScannableItem.getOcrData(),
            dbScannableItem.getFileName(),
            dbScannableItem.getNotes(),
            convertToInputDocumentType(
                dbScannableItem.getDocumentType(),
                dbScannableItem.getDocumentSubtype()
            )
        );
    }

    private InputDocumentType convertToInputDocumentType(
        DocumentType documentType,
        DocumentSubtype documentSubtype
    ) {
        switch (documentType) {
            case CHERISHED:
                return InputDocumentType.CHERISHED;
            case OTHER:
                return documentSubtype == DocumentSubtype.SSCS1
                    ? InputDocumentType.SSCS1
                    : InputDocumentType.OTHER;
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
