package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.applicationinsights.core.dependencies.googlecommon.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataRetrievalFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ServiceJurisdictionMappingConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeRetrieverServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    private EnvelopeRetrieverService envelopeRetrieverService;

    @Mock
    private ServiceJurisdictionMappingConfig mappingConfig;

    @Before
    public void setUp() {
        envelopeRetrieverService = new EnvelopeRetrieverService(envelopeRepository, mappingConfig);
    }

    @Test
    public void should_return_all_envelopes_successfully_for_a_given_jurisdiction() throws Exception {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();

        when(mappingConfig.getServicesJurisdiction()).thenReturn(ImmutableMap.of("testService", "testJurisdiction"));

        when(envelopeRepository.findByJurisdiction("testJurisdiction")).thenReturn(envelopes);

        assertThat(envelopeRetrieverService.getAllEnvelopesForJurisdiction("testService"))
            .containsOnly(envelopes.get(0));

        verify(mappingConfig).getServicesJurisdiction();
        verify(envelopeRepository).findByJurisdiction("testJurisdiction");
    }

    @Test
    public void should_throw_data_retrieval_failure_exception_when_repository_fails_to_retrieve_envelopes() {
        when(mappingConfig.getServicesJurisdiction()).thenReturn(ImmutableMap.of("testService", "testJurisdiction"));
        when(envelopeRepository.findByJurisdiction("testJurisdiction")).thenThrow(DataRetrievalFailureException.class);

        Throwable throwable = catchThrowable(() ->
            envelopeRetrieverService.getAllEnvelopesForJurisdiction("testService"));

        assertThat(throwable).isInstanceOf(DataRetrievalFailureException.class);

        verify(mappingConfig).getServicesJurisdiction();
    }
}
