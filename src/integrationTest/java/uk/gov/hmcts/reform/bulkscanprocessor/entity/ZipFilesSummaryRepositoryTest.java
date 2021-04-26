package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary.ZipFileSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ZipFilesSummaryRepositoryTest {

    @Autowired
    private ZipFilesSummaryRepository reportRepo;
    @Autowired
    private ProcessEventRepository eventRepo;
    @Autowired
    private EnvelopeRepository envelopeRepo;

    @Test
    public void should_return_zipfiles_summary_by_date() {
        // given
        Instant createdDate = Instant.parse("2019-02-15T14:15:23.456Z");
        Instant completedDate = Instant.parse("2019-02-15T14:20:33.656Z");

        dbHasEvents(
            event("c1", "test1.zip", createdDate, ZIPFILE_PROCESSING_STARTED),
            event("c1", "test1.zip", completedDate, COMPLETED),
            event("c2", "test2.zip", createdDate.minus(1, MINUTES), ZIPFILE_PROCESSING_STARTED),
            event("c4", "test4.zip", createdDate.minus(1, HOURS), ZIPFILE_PROCESSING_STARTED),
            event("c4", "test4.zip", createdDate.minus(30, MINUTES), FILE_VALIDATION_FAILURE)
        );
        Envelope e1 = envelope("c1", "test1.zip", Status.COMPLETED, EXCEPTION, "ccd-id-1", "ccd-action-1");
        Envelope e2 = envelope("c2", "test2.zip", Status.CREATED, SUPPLEMENTARY_EVIDENCE, null, null);
        dbHasEnvelope(e1);
        dbHasEnvelope(e2);

        // when
        List<ZipFileSummary> result = reportRepo.getZipFileSummaryReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ZipFileSummaryItem(
                        "test1.zip",
                        createdDate,
                        completedDate,
                        "c1",
                        Event.COMPLETED.toString(),
                        Status.COMPLETED.toString(),
                        EXCEPTION.name(),
                        "ccd-id-1",
                        "ccd-action-1",
                        e1.getId().toString()
                    ),
                    new ZipFileSummaryItem(
                        "test2.zip",
                        createdDate.minus(1, MINUTES),
                        null,
                        "c2",
                        Event.ZIPFILE_PROCESSING_STARTED.toString(),
                        Status.CREATED.toString(),
                        SUPPLEMENTARY_EVIDENCE.name(),
                        null,
                        null,
                        e2.getId().toString()
                    ),
                    new ZipFileSummaryItem(
                        "test4.zip",
                        createdDate.minus(1, HOURS),
                        null,
                        "c4",
                        Event.FILE_VALIDATION_FAILURE.toString(),
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                )
            );
    }

    @Test
    public void should_return_empty_zipfile_summary_when_no_zipfiles_received() {
        // given
        Instant createdAt = Instant.parse("2019-02-09T14:15:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdAt, ZIPFILE_PROCESSING_STARTED),
            event("c1", "test1.zip", createdAt.minus(2, MINUTES), FILE_VALIDATION_FAILURE),
            event("c2", "test2.zip", Instant.parse("2019-02-09T14:15:23.456Z"), ZIPFILE_PROCESSING_STARTED),
            event("c2", "test2.zip", Instant.parse("2019-02-10T14:15:23.456Z"), COMPLETED)
        );

        // when
        List<ZipFileSummary> result = reportRepo.getZipFileSummaryReportFor(LocalDate.of(2019, 2, 10));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_return_zipfilesummary_for_the_requested_date_when_completed_date_is_not_same_as_createdDate() {
        // given
        Instant createdAt = Instant.parse("2019-02-15T23:59:23.456Z");
        Instant nextDay = Instant.parse("2019-02-16T00:00:23.456Z");
        String container = "c1";
        String zip1Name = "test1.zip";

        dbHasEvents(
            event(container, zip1Name, createdAt, ZIPFILE_PROCESSING_STARTED),
            event(container, zip1Name, nextDay, COMPLETED),
            event(container, "test2.zip", nextDay, ZIPFILE_PROCESSING_STARTED)
        );
        var envelope = envelope(container, zip1Name, Status.UPLOADED, SUPPLEMENTARY_EVIDENCE, null, null);
        dbHasEnvelope(envelope);

        // when
        List<ZipFileSummary> result = reportRepo.getZipFileSummaryReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                    new ZipFileSummaryItem(
                        zip1Name,
                        createdAt,
                        nextDay,
                        container,
                        Event.COMPLETED.toString(),
                        Status.UPLOADED.toString(),
                        SUPPLEMENTARY_EVIDENCE.name(),
                        null,
                        null,
                        envelope.getId().toString()
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
            container,
            null
        );

        envelope.setStatus(status);

        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(ccdAction);

        return envelope;
    }
}
