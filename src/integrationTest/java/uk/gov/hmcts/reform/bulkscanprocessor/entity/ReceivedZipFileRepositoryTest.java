package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedZipFileItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ReceivedZipFileRepositoryTest {
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
                    new ReceivedZipFileItem("test2.zip", "c2", createdDate2, null, null, null, null)
                )
            );
    }

    @Test
    void should_return_single_event_by_date_if_envelope_exists() {
        // given
        Instant createdDate1 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant createdDate2 = createdDate1.minus(1, DAYS);
        Instant createdDate3 = createdDate1.plus(1, DAYS);

        dbHasEvents(
            event("c1", "test1.zip", createdDate2, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", createdDate1, ZIPFILE_PROCESSING_STARTED),
            event("c3", "test3.zip", createdDate3, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope existingEnvelope;
        dbHasEnvelope(envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));
        existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);
        dbHasEnvelope(envelope("c3", "test3.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));

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
                        createdDate1,
                        null,
                        null,
                        "test5",
                        existingEnvelope.getId()
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

        Envelope envelope =
            envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test4.zip");
        dbHasEnvelope(envelope);

        dbHasScannableItems(scannableItem(envelope, "doc-1"));
        dbHasPayments(payment(envelope, "pay-1"));

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1", createdDate,
                        "doc-1",
                        "pay-1",
                        "test4.zip",
                        envelope.getId()
                    )
                )
            );
    }

    @Test
    void should_return_multiple_events_if_envelope_exists_with_multiple_scannable_items() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null);
        dbHasEnvelope(envelope);

        dbHasScannableItems(
            scannableItem(envelope, "doc-1"),
            scannableItem(envelope, "doc-2")
        );

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ReceivedZipFileItem("test1.zip", "c1", createdDate, "doc-1", null,  null, envelope.getId()),
                    new ReceivedZipFileItem("test1.zip", "c1", createdDate, "doc-2", null,  null, envelope.getId())
                )
            );
    }

    @Test
    void should_return_multiple_events_if_envelope_exists_with_multiple_payments() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope =
            envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5.zip");
        dbHasEnvelope(envelope);

        dbHasPayments(
            payment(envelope, "pay-1"),
            payment(envelope, "pay-2")
        );

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        null,
                        "pay-1",
                        "test5.zip",
                        envelope.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate,
                        null,
                        "pay-2",
                        "test5.zip",
                        envelope.getId()
                    )
                )
            );
    }

    @Test
    void should_return_multiple_events_if_envelope_exists_with_multiple_payments_and_scannable_items() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope
            = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null);
        dbHasEnvelope(envelope);

        dbHasScannableItems(
            scannableItem(envelope, "doc-1"),
            scannableItem(envelope, "doc-2")
        );
        dbHasPayments(
            payment(envelope, "pay-1"),
            payment(envelope, "pay-2")
        );

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ReceivedZipFileItem("test1.zip", "c1", createdDate, "doc-1", "pay-1",  null, envelope.getId()),
                    new ReceivedZipFileItem("test1.zip", "c1", createdDate, "doc-1", "pay-2", null, envelope.getId()),
                    new ReceivedZipFileItem("test1.zip", "c1", createdDate, "doc-2", "pay-1", null, envelope.getId()),
                    new ReceivedZipFileItem("test1.zip", "c1", createdDate, "doc-2", "pay-2",  null, envelope.getId())
                )
            );
    }

    @Test
    void should_return_multiple_events_if_envelopes_exist_with_multiple_payments_and_scannable_items() {
        // given
        Instant createdDate1 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant createdDate2 = createdDate1.plus(1, HOURS);

        dbHasEvents(
            event("c1", "test1.zip", createdDate1, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", createdDate2, ZIPFILE_PROCESSING_STARTED)
        );

        Envelope envelope1 =
            envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5.zip");
        dbHasEnvelope(envelope1);
        Envelope envelope2 = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null);
        dbHasEnvelope(envelope2);

        dbHasScannableItems(
            scannableItem(envelope1, "doc-1"),
            scannableItem(envelope1, "doc-2"),
            scannableItem(envelope2, "doc-3"),
            scannableItem(envelope2, "doc-4")
        );
        dbHasPayments(
            payment(envelope1, "pay-1"),
            payment(envelope1, "pay-2"),
            payment(envelope2, "pay-3"),
            payment(envelope2, "pay-4")
        );

        // when
        List<ReceivedZipFile> result = reportRepo.getReceivedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate1,
                        "doc-1",
                        "pay-1",
                        "test5.zip",
                        envelope1.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate1,
                        "doc-1",
                        "pay-2",
                        "test5.zip",
                        envelope1.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1", createdDate1,
                        "doc-2",
                        "pay-1",
                        "test5.zip",
                        envelope1.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test1.zip",
                        "c1",
                        createdDate1,
                        "doc-2",
                        "pay-2",
                        "test5.zip",
                        envelope1.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test2.zip",
                        "c2",
                        createdDate2,
                        "doc-3",
                        "pay-3",
                        null,
                        envelope2.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test2.zip",
                        "c2",
                        createdDate2,
                        "doc-3",
                        "pay-4",
                        null,
                        envelope2.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test2.zip",
                        "c2",
                        createdDate2,
                        "doc-4",
                        "pay-3",
                        null,
                        envelope2.getId()
                    ),
                    new ReceivedZipFileItem(
                        "test2.zip",
                        "c2",
                        createdDate2,
                        "doc-4",
                        "pay-4",
                        null,
                        envelope2.getId()
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
        String ccdAction,
        String rescanFor
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
            container,
            rescanFor
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
