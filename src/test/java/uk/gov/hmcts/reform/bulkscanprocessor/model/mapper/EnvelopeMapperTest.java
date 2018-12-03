package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.joda.time.DateTime;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputNonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;

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

        for (int i = 0; i < dbEnvelope.getPayments().size(); i++) {
            Payment dbPayment = dbEnvelope.getPayments().get(i);
            InputPayment zipPayment = zipEnvelope.payments.get(i);

            assertThat(dbPayment)
                .isEqualToIgnoringGivenFields(
                    zipPayment,
                    new String[]{"envelope", "amountInPence", "id"}
                );
        }
    }

    private void assertSameScannableItems(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getScannableItems().size()).isEqualTo(zipEnvelope.scannableItems.size());

        for (int i = 0; i < dbEnvelope.getScannableItems().size(); i++) {
            ScannableItem dbScannableItem = dbEnvelope.getScannableItems().get(i);
            InputScannableItem zipScannableItem = zipEnvelope.scannableItems.get(i);

            assertThat(dbScannableItem)
                .isEqualToIgnoringGivenFields(
                    zipScannableItem,
                    new String[]{"envelope", "documentUrl", "id"}
                );
        }
    }

    private void assertSameNonScannableItems(Envelope dbEnvelope, InputEnvelope zipEnvelope) {
        assertThat(dbEnvelope.getNonScannableItems().size())
            .isEqualTo(zipEnvelope.nonScannableItems.size());

        for (int i = 0; i < dbEnvelope.getNonScannableItems().size(); i++) {
            NonScannableItem dbNonScannableItem = dbEnvelope.getNonScannableItems().get(i);
            InputNonScannableItem zipNonScannableItem = zipEnvelope.nonScannableItems.get(i);

            assertThat(dbNonScannableItem)
                .isEqualToIgnoringGivenFields(
                    zipNonScannableItem,
                    new String[]{"envelope", "id"}
                );
        }
    }
}
