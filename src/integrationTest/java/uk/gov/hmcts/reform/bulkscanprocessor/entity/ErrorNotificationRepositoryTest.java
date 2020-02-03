package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_AV_FAILED;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ErrorNotificationRepositoryTest {

    @Autowired
    private ErrorNotificationRepository repository;

    @Autowired
    private ProcessEventRepository eventRepository;

    @Autowired
    private EntityManager em;

    private long eventId;

    @Before
    public void setUp() {
        eventId = eventRepository.saveAndFlush(
            new ProcessEvent("container", "zip_file_name", Event.DOC_FAILURE)
        ).getId();
    }

    @After
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    public void should_successfully_create_a_record() {
        // given
        ErrorNotification errorNotification = new ErrorNotification(
            em.getReference(ProcessEvent.class, eventId),
            ERR_AV_FAILED.name(),
            "test_service"
        );

        // when
        long dbId = repository.saveAndFlush(errorNotification).getId();

        // then
        ErrorNotification dbItem = repository.getOne(dbId);

        assertThat(dbItem.getProcessEvent().getId()).isEqualTo(eventId);
        assertThat(dbItem.getNotificationId()).isNullOrEmpty();
        assertThat(dbItem.getErrorCode()).isEqualTo(ERR_AV_FAILED.name());

        // and
        assertThat(dbItem.getCreatedAt()).isEqualTo(errorNotification.getCreatedAt());
    }
}
