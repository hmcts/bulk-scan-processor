package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.List;

import static java.util.Arrays.asList;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ProcessEventRepositoryTest {

    @Autowired
    private ProcessEventRepository repo;

    @Test
    public void findByZipFileName_should_find_events_in_db() {
        // given
        repo.saveAll(asList(
            new ProcessEvent("A", "hello.zip", Event.DOC_UPLOADED),
            new ProcessEvent("B", "hello.zip", Event.DOC_UPLOADED),
            new ProcessEvent("C", "world.zip", Event.DOC_UPLOADED)
        ));

        // when
        List<ProcessEvent> resultForHello = repo.findByZipFileName("hello.zip");
        List<ProcessEvent> resultForX = repo.findByZipFileName("x.zip");

        // then
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(resultForHello)
            .usingElementComparatorOnFields("container", "zipFileName", "event")
            .containsExactlyInAnyOrder(
                new ProcessEvent("A", "hello.zip", Event.DOC_UPLOADED),
                new ProcessEvent("B", "hello.zip", Event.DOC_UPLOADED)
            );

        softly.assertThat(resultForX).hasSize(0);
    }
}
