package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.OTHER;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ReceivedZipFileRepositoryTest {
    private static final Logger log = LoggerFactory.getLogger(ReceivedZipFileRepositoryTest.class);

    @Autowired
    private ReceivedZipFileRepository reportRepo;
    @Autowired
    private ProcessEventRepository eventRepo;
    @Autowired
    private EnvelopeRepository envelopeRepo;
    @Autowired
    private ScannableItemRepository scannableItemRepo;
    @Autowired
    private PaymentRepository paymentRepo;

    @Test
    void should_return_single_event_if_no_envelope() {
        // given
        Instant createdDate1 = Instant.parse("2019-02-14T14:15:23.456Z");
        Instant createdDate2 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant createdDate3 = Instant.parse("2019-02-16T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate1, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", createdDate2, ZIPFILE_PROCESSING_STARTED),
            event("c3", "test3.zip", createdDate3, ZIPFILE_PROCESSING_STARTED)
        );

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                    new ReceivedZipFileItem(
                        "test2.zip",
                        "c2",
                        createdDate2,
                        null,
                        null
                    )
                )
            );
    }

    @Test
    void should_return_single_event_by_date_if_envelope_exists() {
        // given
        Instant createdDate1 = Instant.parse("2019-02-14T14:15:23.456Z");
        Instant createdDate2 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant createdDate3 = Instant.parse("2019-02-16T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate1, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", createdDate2, ZIPFILE_PROCESSING_STARTED),
            event("c3", "test3.zip", createdDate3, ZIPFILE_PROCESSING_STARTED)
        );

        dbHasEnvelope(envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1"));
        dbHasEnvelope(envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1"));
        dbHasEnvelope(envelope("c3", "test3.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1"));

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                    new ReceivedZipFileItem(
                        "test2.zip",
                        "c2",
                        createdDate2,
                        null,
                        null
                    )
                )
            );
    }

    @Test
    void should_return_single_event_if_envelope_exists_with_payment_and_scannable_item() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1");
        dbHasEnvelope(envelope);

        dbHasScannableItems(scannableItem(envelope, "dcn1"));
        dbHasPayments(payment(envelope, "dcn2"));

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                singletonList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn1",
                        "dcn2"
                    )
                )
            );
    }

    @Test
    void should_return_single_event_if_envelope_exists_with_multiple_scannable_items() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1");
        dbHasEnvelope(envelope);

        dbHasScannableItems(scannableItem(envelope, "dcn1"), scannableItem(envelope, "dcn2"));

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                asList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn1",
                        null
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn1",
                        null
                    )
                )
            );
    }

    @Test
    void should_return_single_event_if_envelope_exists_with_multiple_payments() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1");
        dbHasEnvelope(envelope);

        dbHasPayments(payment(envelope, "dcn3"),payment(envelope, "dcn4"));

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                asList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        null,
                        "dcn3"
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        null,
                        "dcn4"
                    )
                )
            );
    }

    @Test
    void should_return_single_event_if_envelope_exists_with_multiple_payments_and_scannable_items() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1");
        dbHasEnvelope(envelope);

        dbHasScannableItems(scannableItem(envelope, "dcn1"), scannableItem(envelope, "dcn2"));
        dbHasPayments(payment(envelope, "dcn3"),payment(envelope, "dcn4"));

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(
                asList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn1",
                        "dcn3"
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn1",
                        "dcn4"
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn2",
                        "dcn3"
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        "dcn2",
                        "dcn4"
                    )
                )
            );
    }

    private void dbHasEvents(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
    }

    private void dbHasEnvelope(Envelope envelope) {
        envelopeRepo.save(envelope);
    }

    private void dbHasScannableItems(ScannableItem... scannableItems) {
        scannableItemRepo.saveAll(asList(scannableItems));
    }

    private void dbHasPayments(Payment... payments) {
        paymentRepo.saveAll(asList(payments));
    }

    private ProcessEvent event(String container, String zipFileName, Instant createdAt, Event type) {
        ProcessEvent event = new ProcessEvent(container, zipFileName, type);
        event.setCreatedAt(createdAt);

        return event;
    }

    private Envelope envelope(
        String container,
        String zipFileName,
        Status status,
        Classification classification,
        String ccdId,
        String ccdAction
    ) {
        Envelope envelope = new Envelope(
            UUID.randomUUID().toString(),
            "jurisdiction1",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            zipFileName,
            "1234432112344321",
            null,
            classification,
            emptyList(),
            emptyList(),
            emptyList(),
            container
        );

        envelope.setStatus(status);

        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(ccdAction);

        return envelope;
    }

    private ScannableItem scannableItem(Envelope envelope, String dcn) {
        ScannableItem scannableItem = new ScannableItem(
            dcn,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            OTHER,
            null,
            null
        );
        scannableItem.setEnvelope(envelope);
        return scannableItem;
    }

    private Payment payment(Envelope envelope, String dcn) {
        Payment payment = new Payment(dcn);
        payment.setEnvelope(envelope);
        return payment;
    }
}
