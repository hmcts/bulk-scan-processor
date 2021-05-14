package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_SIGNATURE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ProcessEventRepositoryTest {

    @Autowired
    private ProcessEventRepository repo;

    @Test
    public void findByZipFileName_should_find_events_in_db() {
        // given
        repo.saveAll(asList(
            new ProcessEvent("A", "hello.zip", DOC_UPLOADED),
            new ProcessEvent("B", "hello.zip", DOC_UPLOADED),
            new ProcessEvent("C", "world.zip", DOC_UPLOADED)
        ));

        // when
        List<ProcessEvent> resultForHello = repo.findByZipFileName("hello.zip");
        List<ProcessEvent> resultForX = repo.findByZipFileName("x.zip");

        // then
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(resultForHello)
            .usingElementComparatorOnFields("container", "zipFileName", "event")
            .containsExactlyInAnyOrder(
                new ProcessEvent("A", "hello.zip", DOC_UPLOADED),
                new ProcessEvent("B", "hello.zip", DOC_UPLOADED)
            );

        softly.assertThat(resultForX).hasSize(0);
    }

    @Test
    public void getRejectionEvents_should_return_rejection_events_for_date() {
        // given
        LocalDate queryDate = LocalDate.parse("2021-05-10");
        Instant queryInstant = queryDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        ProcessEvent ev0 = getProcessEvent("A", "file1.zip", ZIPFILE_PROCESSING_STARTED, "2021-05-10T14:15:23.456Z");
        ProcessEvent rejectionInQueryDateEv1 = getProcessEvent(
            "A",
            "file1.zip",
            DOC_FAILURE,
            "2021-05-10T14:16:23.456Z"
        );
        ProcessEvent ev2 = getProcessEvent("A", "file1.zip", ZIPFILE_PROCESSING_STARTED, "2021-05-10T14:17:23.456Z");
        ProcessEvent ev3 = getProcessEvent("A", "file1.zip", DOC_UPLOADED, "2021-05-10T14:18:23.456Z");
        ProcessEvent ev4 = getProcessEvent(
            "A",
            "file1.zip",
            DOC_PROCESSED_NOTIFICATION_SENT,
            "2021-05-10T14:19:23.456Z"
        );
        ProcessEvent ev5 = getProcessEvent("A", "file1.zip", COMPLETED, "2021-05-10T14:20:23.456Z");
        ProcessEvent ev6 = getProcessEvent("B", "file2.zip", ZIPFILE_PROCESSING_STARTED, "2021-05-10T14:21:23.456Z");
        ProcessEvent rejectionInQueryDateEv7 = getProcessEvent(
            "B",
            "file2.zip",
            FILE_VALIDATION_FAILURE,
            "2021-05-10T14:21:22.456Z"
        );
        ProcessEvent ev8 = getProcessEvent("B", "file3.zip", ZIPFILE_PROCESSING_STARTED, "2021-05-10T14:23:23.456Z");
        ProcessEvent rejectionInQueryDateEv9 = getProcessEvent(
            "B",
            "file3.zip",
            DOC_SIGNATURE_FAILURE,
            "2021-05-10T14:24:23.456Z"
        );
        ProcessEvent ev10 = getProcessEvent("C", "file4.zip", ZIPFILE_PROCESSING_STARTED, "2021-05-08T14:25:23.456Z");
        ProcessEvent rejectionBeforeQueryDateEv11 = getProcessEvent(
            "C",
            "file4.zip",
            DOC_FAILURE,
            "2021-05-08T14:26:23.456Z"
        );
        ProcessEvent ev12 = getProcessEvent("C", "file5.zip", ZIPFILE_PROCESSING_STARTED, "2021-05-09T14:27:23.456Z");
        ProcessEvent rejectionAfterQueryDateEv13 = getProcessEvent(
            "C",
            "file5.zip",
            DOC_SIGNATURE_FAILURE,
            "2021-05-09T14:24:28.456Z"
        );
        repo.saveAll(asList(
            ev0,
            rejectionInQueryDateEv1,
            ev2,
            ev3,
            ev4,
            ev5,
            ev6,
            rejectionInQueryDateEv7,
            ev8,
            rejectionInQueryDateEv9,
            ev10,
            rejectionBeforeQueryDateEv11,
            ev12,
            rejectionAfterQueryDateEv13
        ));

        // when
        List<ProcessEvent> res = repo.getRejectionEvents(queryInstant);

        // then
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(res)
            .usingElementComparatorOnFields("container", "zipFileName", "event")
            .containsExactlyInAnyOrder(
                new ProcessEvent("A", "file1.zip", DOC_FAILURE),
                new ProcessEvent("B", "file2.zip", FILE_VALIDATION_FAILURE),
                new ProcessEvent("B", "file3.zip", DOC_SIGNATURE_FAILURE)
            );
    }

    private ProcessEvent getProcessEvent(String container, String fileName, Event event, String createdAt) {
        ProcessEvent ev = new ProcessEvent(container, fileName, event);
        ev.setCreatedAt(Instant.parse(createdAt));
        return ev;
    }
}
