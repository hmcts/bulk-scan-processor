package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataRetrievalFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeRetrieverServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    private EnvelopeRetrieverService envelopeRetrieverService;

    @Before
    public void setUp() {
        envelopeRetrieverService = new EnvelopeRetrieverService(envelopeRepository);
    }

    @Test
    public void should_return_all_envelopes_successfully() {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();

        when(envelopeRepository.findAll()).thenReturn(envelopes);

        assertThat(envelopeRetrieverService.getAllEnvelopes()).containsOnly(envelopes.get(0));
    }

    @Test
    public void should_throw_data_retrieval_failure_exception_when_repository_fails_to_retrieve_envelopes() {
        when(envelopeRepository.findAll()).thenThrow(DataRetrievalFailureException.class);

        Throwable throwable = catchThrowable(() -> envelopeRetrieverService.getAllEnvelopes());

        assertThat(throwable).isInstanceOf(DataRetrievalFailureException.class);
    }
}
