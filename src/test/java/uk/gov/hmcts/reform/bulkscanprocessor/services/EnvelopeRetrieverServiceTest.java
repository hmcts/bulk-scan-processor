package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataRetrievalFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeRetrieverServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    private EnvelopeRetrieverService envelopeRetrieverService;

    @Mock
    private EnvelopeAccessProperties envelopeAccess;

    @Before
    public void setUp() {
        envelopeRetrieverService = new EnvelopeRetrieverService(envelopeRepository, envelopeAccess);
    }

    @Test
    public void should_return_all_envelopes_successfully_for_a_given_jurisdiction_and_status() throws Exception {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();

        when(envelopeAccess.getMappings())
            .thenReturn(singletonList(new Mapping("testJurisdiction", "testService", "testService")));

        when(envelopeRepository.findByJurisdictionAndStatus("testJurisdiction", PROCESSED))
            .thenReturn(envelopes);

        assertThat(envelopeRetrieverService.findByServiceAndStatus("testService", PROCESSED))
            .containsOnly(envelopes.get(0));

        verify(envelopeRepository).findByJurisdictionAndStatus("testJurisdiction", PROCESSED);
    }

    @Test
    public void should_return_all_envelopes_if_status_is_null() throws Exception {
        // given
        List<Envelope> envelopes =
            asList(
                envelope(),
                envelope(),
                envelope()
            );

        given(envelopeAccess.getMappings())
            .willReturn(singletonList(new Mapping("testJurisdiction", "testService", "testService")));

        given(envelopeRepository.findByJurisdiction("testJurisdiction"))
            .willReturn(envelopes);

        // when
        List<Envelope> foundEnvelopes = envelopeRetrieverService.findByServiceAndStatus("testService", null);

        // then
        assertThat(foundEnvelopes).containsExactlyInAnyOrderElementsOf(envelopes);
    }

    @Test
    public void should_throw_data_retrieval_failure_exception_when_repository_fails_to_retrieve_envelopes() {
        when(envelopeAccess.getMappings())
            .thenReturn(singletonList(new Mapping("testJurisdiction", "testService", "testService")));

        when(envelopeRepository.findByJurisdictionAndStatus("testJurisdiction", PROCESSED))
            .thenThrow(DataRetrievalFailureException.class);

        Throwable throwable = catchThrowable(() ->
            envelopeRetrieverService.findByServiceAndStatus("testService", PROCESSED));

        assertThat(throwable).isInstanceOf(DataRetrievalFailureException.class);
    }
}
