package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedZipFileItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_SIGNATURE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class RejectedZipFileRepositoryTest {
    @Autowired
    private RejectedZipFileRepository reportRepo;
    @Autowired
    private ProcessEventRepository eventRepo;
    @Autowired
    private EnvelopeRepository envelopeRepo;

    @Test
    void should_return_single_result_if_no_envelopes() {
        // given
        Instant createdDate11 = Instant.parse("2019-02-14T14:15:23.456Z");
        Instant createdDate12 = Instant.parse("2019-02-14T14:15:26.456Z");
        Instant createdDate21 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant createdDate22 = Instant.parse("2019-02-15T14:15:26.456Z");
        Instant createdDate31 = Instant.parse("2019-02-16T14:15:23.456Z");
        Instant createdDate32 = Instant.parse("2019-02-16T14:15:26.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate11, ZIPFILE_PROCESSING_STARTED),
            event("c1", "test1.zip", createdDate12, FILE_VALIDATION_FAILURE),
            event("c2", "test2.zip", createdDate21, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", createdDate22, FILE_VALIDATION_FAILURE),
            event("c3", "test3.zip", createdDate31, ZIPFILE_PROCESSING_STARTED),
            event("c3", "test3.zip", createdDate32, FILE_VALIDATION_FAILURE)
        );

        // when
        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                    new RejectedZipFileItem(
                            "test2.zip",
                            "c2",
                            createdDate22,
                            null,
                            "FILE_VALIDATION_FAILURE"
                    )
                )
            );
    }

    @Test
    void should_return_single_result_by_date_if_envelope_exists() {
        // given
        Instant eventDate11 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant eventDate12 = Instant.parse("2019-02-15T14:15:26.456Z");
        Instant eventDate21 = eventDate11.minus(1, DAYS);
        Instant eventDate22 = eventDate12.minus(1, DAYS);
        Instant eventDate31 = eventDate11.plus(1, DAYS);
        Instant eventDate32 = eventDate12.plus(1, DAYS);

        dbHasEvents(
            event("c1", "test1.zip", eventDate21, ZIPFILE_PROCESSING_STARTED),
            event("c1", "test1.zip", eventDate22, DOC_FAILURE),
            event("c2", "test2.zip", eventDate11, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", eventDate12, DOC_FAILURE),
            event("c3", "test3.zip", eventDate31, ZIPFILE_PROCESSING_STARTED),
            event("c3", "test3.zip", eventDate32, DOC_FAILURE)
        );

        dbHasEnvelope(envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));
        Envelope existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);
        dbHasEnvelope(envelope("c3", "test3.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));

        // when
        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                        new RejectedZipFileItem(
                                "test2.zip",
                                "c2",
                                eventDate12,
                                existingEnvelope.getId(),
                                "DOC_FAILURE"
                        )
                )
            );
    }

    @Test
    void should_return_single_result_by_date_if_envelope_exists_with_multiple_failure_events() {
        // given
        Instant eventDate11 = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant eventDate12 = Instant.parse("2019-02-15T14:16:23.456Z");
        Instant eventDate13 = Instant.parse("2019-02-15T14:17:23.456Z");
        Instant eventDate14 = Instant.parse("2019-02-15T14:18:23.456Z");

        dbHasEvents(
            event("c2", "test2.zip", eventDate11, ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", eventDate12, DOC_FAILURE),
            event("c2", "test2.zip", eventDate13, DOC_FAILURE),
            event("c2", "test2.zip", eventDate14, DOC_FAILURE)
        );

        dbHasEnvelope(envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));
        Envelope existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);
        dbHasEnvelope(envelope("c3", "test3.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));

        // when
        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                        new RejectedZipFileItem(
                                "test2.zip",
                                "c2",
                                eventDate12,
                                existingEnvelope.getId(),
                                "DOC_FAILURE"
                        )
                )
            );
    }

    @Test
    public void should_return_rejected_zip_files_with_matching_name() {
        Instant eventDate = Instant.parse("2019-02-15T14:15:23.456Z");

        dbHasEvents(
            event("c2", "test2.zip", eventDate, DOC_FAILURE),
            event("c2", "test2.zip", eventDate, FILE_VALIDATION_FAILURE),
            event("c2", "test2.zip", eventDate, DOC_SIGNATURE_FAILURE),
            event("c2", "test3.zip", eventDate, FILE_VALIDATION_FAILURE)
        );

        Envelope existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);
        dbHasEnvelope(envelope("c3", "test3.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));

        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor("test2.zip");

        assertThat(result)
            .hasSize(3)
            .extracting("zipFileName", "event")
            .contains(tuple("test2.zip", "DOC_FAILURE"),
                      tuple("test2.zip", "FILE_VALIDATION_FAILURE"),
                      tuple("test2.zip", "DOC_SIGNATURE_FAILURE"))
            .doesNotContain(tuple("test3.zip", "FILE_VALIDATION_FAILURE"));
    }

    @Test
    public void should_group_rejected_zip_files_with_matching_name_if_same_event_same_day() {
        Instant eventDate = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant eventDate2 = Instant.parse("2019-02-16T14:15:23.456Z");
        dbHasEvents(
            event("c2", "test2.zip", eventDate, FILE_VALIDATION_FAILURE),
            event("c2", "test2.zip", eventDate, FILE_VALIDATION_FAILURE),
            event("c2", "test2.zip", eventDate2, FILE_VALIDATION_FAILURE),
            event("c2", "test2.zip", eventDate2, FILE_VALIDATION_FAILURE)
        );

        Envelope existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);
        dbHasEnvelope(envelope("c3", "test3.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", null));

        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor("test2.zip");

        assertThat(result)
            .hasSize(2)
            .extracting("zipFileName", "event")
            .contains(tuple("test2.zip", "FILE_VALIDATION_FAILURE"), tuple("test2.zip", "FILE_VALIDATION_FAILURE"));
    }

    @Test
    public void should_not_return_rejected_zip_files_with_matching_name_if_not_failure_event() {
        Instant eventDate = Instant.parse("2019-02-15T14:15:23.456Z");
        dbHasEvents(
            event("c2", "test2.zip", eventDate, ZIPFILE_PROCESSING_STARTED)
        );
        Envelope existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);

        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor("test2.zip");

        assertThat(result).isEmpty();
    }

    @Test
    public void should_not_return_rejected_zip_files_if_no_event_with_matching_name() {
        Instant eventDate = Instant.parse("2019-02-15T14:15:23.456Z");
        dbHasEvents(
            event("c2", "test2.zip", eventDate, FILE_VALIDATION_FAILURE)
        );
        Envelope existingEnvelope
            = envelope("c2", "test2.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1", "test5");
        dbHasEnvelope(existingEnvelope);

        List<RejectedZipFile> result = reportRepo.getRejectedZipFilesReportFor("test22.zip");

        assertThat(result).isEmpty();
    }

    private void dbHasEvents(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
    }

    private void dbHasEnvelope(Envelope envelope) {
        envelopeRepo.save(envelope);
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
}
