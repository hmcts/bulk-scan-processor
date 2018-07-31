package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EnvelopeUpdateServiceTest {

    @Autowired EnvelopeRepository envelopeRepo;
    @Autowired ProcessEventRepository eventRepo;

    @Autowired EnvelopeUpdateService envelopeUpdateService;

    @Test
    public void should_update_envelope_status_and_create_an_event() throws Exception {
        // given
        Envelope envelope = envelopeRepo.saveAndFlush(EnvelopeCreator.envelope());

        // when
        envelopeUpdateService.updateStatus(
            envelope.getId(),
            Status.CONSUMED,
            "update_service" // see config for tests
        );

        // then
        assertThat(envelopeRepo.findById(envelope.getId()))
            .hasValueSatisfying(env -> {
                assertThat(env.getStatus()).isEqualTo(Status.CONSUMED);
            });

        assertThat(eventRepo.findAll())
            .hasOnlyOneElementSatisfying(evnt -> {
                assertThat(evnt.getEvent()).isEqualTo(Event.DOC_CONSUMED);
                assertThat(evnt.getZipFileName()).isEqualTo(envelope.getZipFileName());
            });
    }

    @After
    public void tearDown() throws Exception {
        eventRepo.deleteAll();
        envelopeRepo.deleteAll();
    }
}
