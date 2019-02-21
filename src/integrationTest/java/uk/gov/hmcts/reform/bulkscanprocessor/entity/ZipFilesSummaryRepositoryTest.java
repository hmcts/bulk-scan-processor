package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary.Item;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
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

        // when
        List<ZipFileSummaryItem> result = reportRepo.getZipFileSummaryReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new Item("test1.zip", createdDate, completedDate, "c1", COMPLETED.toString()),
                    new Item(
                        "test2.zip", createdDate.minus(1, MINUTES), null, "c2", ZIPFILE_PROCESSING_STARTED.toString()
                    ),
                    new Item("test4.zip", createdDate.minus(1, HOURS), null, "c4", FILE_VALIDATION_FAILURE.toString())
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
        List<ZipFileSummaryItem> result = reportRepo.getZipFileSummaryReportFor(LocalDate.of(2019, 2, 10));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_return_zipfilesummary_for_the_requested_date_when_completed_date_is_not_same_as_createdDate() {
        // given
        Instant createdAt = Instant.parse("2019-02-15T23:59:23.456Z");
        Instant nextDay = Instant.parse("2019-02-16T00:00:23.456Z");

        dbHasEvents(
            event("c1", "test1.zip", createdAt, ZIPFILE_PROCESSING_STARTED),
            event("c1", "test1.zip", nextDay, COMPLETED),
            event("c1", "test2.zip", nextDay, ZIPFILE_PROCESSING_STARTED)
        );

        // when
        List<ZipFileSummaryItem> result = reportRepo.getZipFileSummaryReportFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                singletonList(
                    new Item("test1.zip", createdAt, nextDay, "c1", COMPLETED.toString())
                )
            );
    }

    private void dbHasEvents(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
    }

    private ProcessEvent event(String container, String zipFileName, Instant createdAt, Event type) {
        ProcessEvent event = new ProcessEvent(container, zipFileName, type);
        event.setCreatedAt(createdAt);

        return event;
    }
}
