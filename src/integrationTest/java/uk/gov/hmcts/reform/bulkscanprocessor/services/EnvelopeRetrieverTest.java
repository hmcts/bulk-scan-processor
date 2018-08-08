package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EnvelopeRetrieverTest {

    @Autowired private EnvelopeRepository envelopeRepo;
    @Mock EnvelopeAccessProperties accessProperties;

    private EnvelopeRetrieverService service;

    @Before
    public void setUp() throws Exception {
        this.service = new EnvelopeRetrieverService(envelopeRepo, accessProperties);
    }

    @Test
    public void should_retrieve_envelopes_by_status_and_jurisdiction() throws Exception {
        // given
        dbContains(
            envelope("A", Status.PROCESSED),
            envelope("A", Status.PROCESSED),
            envelope("A", Status.CONSUMED),
            envelope("B", Status.PROCESSED),
            envelope("B", Status.CONSUMED)
        );

        // and
        serviceCanReadFromJurisdiction("service_A", "A");

        // when
        List<Envelope> envs = service.findByServiceAndStatus("service_A", Status.PROCESSED);

        // then
        assertThat(envs).hasSize(2);
        assertThat(envs).allMatch(e -> e.getJurisdiction().equals("A"));
        assertThat(envs).allMatch(e -> e.getStatus() == Status.PROCESSED);
    }

    @Test
    public void should_return_empty_list_if_no_envelopes_for_given_jurisdiction_and_status_are_found()
        throws Exception {

        // given
        dbContains(
            envelope("A", Status.PROCESSED),
            envelope("A", Status.CONSUMED),
            envelope("B", Status.PROCESSED),
            envelope("B", Status.PROCESSED)
        );

        // and
        serviceCanReadFromJurisdiction("service_B", "B");

        // when
        List<Envelope> envs = service.findByServiceAndStatus("service_B", Status.CONSUMED);

        // then
        assertThat(envs).hasSize(0);
    }

    @Test
    public void should_retrieve_all_envelopes_for_given_jurisdiction_if_passed_status_is_null() throws Exception {
        // given
        dbContains(
            envelope("X", Status.PROCESSED),
            envelope("X", Status.PROCESSED),
            envelope("X", Status.CONSUMED),
            envelope("X", Status.UPLOADED),
            envelope("Y", Status.CONSUMED)
        );

        // and
        serviceCanReadFromJurisdiction("service_X", "X");

        // when
        List<Envelope> envs = service.findByServiceAndStatus("service_X", null);

        // then
        assertThat(envs).hasSize(4);
        assertThat(envs).allMatch(e -> e.getJurisdiction().equals("X"));
    }

    @After
    public void tearDown() throws Exception {
        envelopeRepo.deleteAll();
    }

    private void dbContains(Envelope... envelopes) {
        for (Envelope env : envelopes) {
            envelopeRepo.save(env);
        }
    }

    private void serviceCanReadFromJurisdiction(String serviceName, String jurisdiction) {
        given(this.accessProperties.getMappings())
            .willReturn(singletonList(new Mapping(jurisdiction, serviceName, serviceName)));
    }
}
