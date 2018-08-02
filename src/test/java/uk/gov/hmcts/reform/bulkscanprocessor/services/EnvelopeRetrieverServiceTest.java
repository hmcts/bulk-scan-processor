package uk.gov.hmcts.reform.bulkscanprocessor.services;

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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeRetrieverServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private EnvelopeAccessProperties envelopeAccess;

    @Test
    public void should_return_all_envelopes_successfully_for_a_given_jurisdiction() throws Exception {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();

        when(envelopeAccess.getMappings())
            .thenReturn(singletonList(new Mapping("testJurisdiction", "testService", "testService")));

        when(envelopeRepository.findByJurisdictionAndStatus("testJurisdiction", PROCESSED))
            .thenReturn(envelopes);

        EnvelopeRetrieverService envelopeRetrieverService = getRetrieverService();

        assertThat(envelopeRetrieverService.getProcessedEnvelopesByJurisdiction("testService"))
            .containsOnly(envelopes.get(0));

        verify(envelopeRepository).findByJurisdictionAndStatus("testJurisdiction", PROCESSED);
    }

    @Test
    public void should_throw_data_retrieval_failure_exception_when_repository_fails_to_retrieve_envelopes() {
        when(envelopeAccess.getMappings())
            .thenReturn(singletonList(new Mapping("testJurisdiction", "testService", "testService")));

        when(envelopeRepository.findByJurisdictionAndStatus("testJurisdiction", PROCESSED))
            .thenThrow(DataRetrievalFailureException.class);

        EnvelopeRetrieverService envelopeRetrieverService = getRetrieverService();

        Throwable throwable = catchThrowable(() ->
            envelopeRetrieverService.getProcessedEnvelopesByJurisdiction("testService"));

        assertThat(throwable).isInstanceOf(DataRetrievalFailureException.class);
    }

    private EnvelopeRetrieverService getRetrieverService() {
        return new EnvelopeRetrieverService(envelopeRepository, new EnvelopeAccessService(envelopeAccess));
    }
}
