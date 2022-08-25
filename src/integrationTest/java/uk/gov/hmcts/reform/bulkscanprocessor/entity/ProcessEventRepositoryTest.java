package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

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
            getProcessEvent(
                "A",
                "hello.zip",
                Event.DOC_UPLOADED,
                "2021-02-07T14:16:23Z"
            ),
            getProcessEvent(
                "B",
                "hello.zip",
                Event.DOC_UPLOADED,
                "2021-02-07T14:15:23Z"
            ),
            getProcessEvent(
                "C",
                "world.zip",
                Event.DOC_UPLOADED,
                "2021-02-07T14:15:23Z"
            )
        ));

        // when
        List<ProcessEvent> res = repo.findByZipFileNameOrderByCreatedAtDesc("hello.zip");
        List<ProcessEvent> resultForX = repo.findByZipFileNameOrderByCreatedAtDesc("x.zip");

        // then
        assertThat(res)
            .usingRecursiveFieldByFieldElementComparatorOnFields("container", "zipFileName", "event", "createdAt")
            .containsExactly(
                getProcessEvent(
                    "A",
                    "hello.zip",
                    Event.DOC_UPLOADED,
                    "2021-02-07T14:16:23Z"
                ),
                getProcessEvent(
                    "B",
                    "hello.zip",
                    Event.DOC_UPLOADED,
                    "2021-02-07T14:15:23Z"
                )
            );

        assertThat(resultForX).hasSize(0);
    }

    @Test
    public void findEventsByDcnPrefix_should_find_event_in_db() {
        // given
        saveEvents();

        // when
        List<ProcessEvent> res = repo.findEventsByDcnPrefix(
                "2103404021053",
                LocalDate.ofInstant(Instant.parse("2021-02-02T14:15:23Z"), ZoneOffset.UTC),
                LocalDate.ofInstant(Instant.parse("2021-02-10T14:15:23Z"), ZoneOffset.UTC)
        );

        // then
        assertThat(res)
            .usingRecursiveFieldByFieldElementComparatorOnFields("container", "zipFileName", "event", "createdAt")
            .containsExactly(
                getProcessEvent(
                    "B",
                    "2103404021053_07-02-2021-13-11-17.zip",
                    Event.FILE_VALIDATION_FAILURE,
                    "2021-02-07T14:15:39Z"
                ),
                getProcessEvent(
                    "B",
                    "2103404021053_07-02-2021-13-11-17.zip",
                    Event.ZIPFILE_PROCESSING_STARTED,
                    "2021-02-07T14:15:23Z"
                )
            );
    }

    @Test
    public void findEventsByDcnPrefix_should_find_several_events_in_db() {
        // given
        saveEvents();

        // when
        List<ProcessEvent> res = repo.findEventsByDcnPrefix(
                "210340402105",
                LocalDate.ofInstant(Instant.parse("2021-02-04T14:15:23Z"), ZoneOffset.UTC),
                LocalDate.ofInstant(Instant.parse("2021-02-08T14:15:23Z"), ZoneOffset.UTC)
        );

        // then
        assertThat(res)
            .usingRecursiveFieldByFieldElementComparatorOnFields("container", "zipFileName", "event", "createdAt")
            .containsExactly(
                getProcessEvent(
                    "A",
                    "2103404021052_05-02-2021-13-11-17.zip",
                    Event.FILE_VALIDATION_FAILURE,
                    "2021-02-05T14:15:39Z"
                ),
                getProcessEvent(
                    "A",
                    "2103404021052_05-02-2021-13-11-17.zip",
                    Event.ZIPFILE_PROCESSING_STARTED,
                    "2021-02-05T14:15:23Z"
                ),
                getProcessEvent(
                    "B",
                    "2103404021053_07-02-2021-13-11-17.zip",
                    Event.FILE_VALIDATION_FAILURE,
                    "2021-02-07T14:15:39Z"
                ),
                getProcessEvent(
                    "B",
                    "2103404021053_07-02-2021-13-11-17.zip",
                    Event.ZIPFILE_PROCESSING_STARTED,
                    "2021-02-07T14:15:23Z"
                )
            );
    }

    @Test
    public void findEventsByDcnPrefix_should_find_no_events_in_db() {
        // given
        saveEvents();

        // when
        List<ProcessEvent> res = repo.findEventsByDcnPrefix(
                "210340402105",
                LocalDate.ofInstant(Instant.parse("2021-01-04T14:15:23Z"), ZoneOffset.UTC),
                LocalDate.ofInstant(Instant.parse("2021-01-08T14:15:23Z"), ZoneOffset.UTC)
        );

        // then
        assertThat(res).isEmpty();
    }

    private void saveEvents() {
        ProcessEvent e11 = getProcessEvent(
            "A",
            "2103404021051_2021-02-03-13-11-17.zip",
            Event.ZIPFILE_PROCESSING_STARTED,
            "2021-02-03T14:15:23Z"
        );
        ProcessEvent e12 = getProcessEvent(
            "A",
            "2103404021051_2021-02-03-13-11-17.zip",
            Event.FILE_VALIDATION_FAILURE,
            "2021-02-03T14:15:39Z"
        );
        ProcessEvent e21 = getProcessEvent(
            "A",
            "2103404021052_05-02-2021-13-11-17.zip",
            Event.ZIPFILE_PROCESSING_STARTED,
            "2021-02-05T14:15:23Z"
        );
        ProcessEvent e22 = getProcessEvent(
            "A",
            "2103404021052_05-02-2021-13-11-17.zip",
            Event.FILE_VALIDATION_FAILURE,
            "2021-02-05T14:15:39Z"
        );
        ProcessEvent e31 = getProcessEvent(
            "B",
            "2103404021053_07-02-2021-13-11-17.zip",
            Event.ZIPFILE_PROCESSING_STARTED,
            "2021-02-07T14:15:23Z"
        );
        ProcessEvent e32 = getProcessEvent(
            "B",
            "2103404021053_07-02-2021-13-11-17.zip",
            Event.FILE_VALIDATION_FAILURE,
            "2021-02-07T14:15:39Z"
        );
        ProcessEvent e41 = getProcessEvent(
            "B",
            "2103404021054_09-02-2021-13-11-17.zip",
            Event.ZIPFILE_PROCESSING_STARTED,
            "2021-02-09T14:15:23Z"
        );
        ProcessEvent e42 = getProcessEvent(
            "B",
            "2103404021054_09-02-2021-13-11-17.zip",
            Event.FILE_VALIDATION_FAILURE,
            "2021-02-09T14:15:39Z"
        );
        repo.saveAll(asList(e11, e12, e21, e22, e31, e32, e41, e42));
    }

    @NotNull
    private ProcessEvent getProcessEvent(
            String container,
            String zipFileName,
            Event event,
            String createdAt
    ) {
        ProcessEvent processEvent = new ProcessEvent(
            container,
            zipFileName,
            event
        );

        processEvent.setCreatedAt(Instant.parse(createdAt));

        return processEvent;
    }
}
