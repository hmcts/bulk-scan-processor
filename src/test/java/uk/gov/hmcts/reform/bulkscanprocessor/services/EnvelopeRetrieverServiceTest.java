package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@ExtendWith(MockitoExtension.class)
class EnvelopeRetrieverServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    private EnvelopeRetrieverService envelopeRetrieverService;

    @Mock
    private EnvelopeAccessService envelopeAccess;

    @BeforeEach
    public void setUp() {
        envelopeRetrieverService = new EnvelopeRetrieverService(envelopeRepository, envelopeAccess);
    }

    @Test
    void should_return_all_envelopes_successfully_for_a_given_jurisdiction_and_status() throws Exception {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();
        List<EnvelopeResponse> envelopesResponse = EnvelopeResponseMapper.toEnvelopesResponse(envelopes);

        when(envelopeAccess.getReadJurisdictionForService("testService"))
            .thenReturn("testJurisdiction");

        when(envelopeRepository
                 .findByJurisdictionAndStatusAndCreatedAtGreaterThan(eq("testJurisdiction"), eq(UPLOADED), any()))
            .thenReturn(envelopes);

        List<EnvelopeResponse> retrievedResponses =
            envelopeRetrieverService.findByServiceAndStatus("testService", UPLOADED);
        // then
        assertThat(retrievedResponses)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(envelopesResponse);
        var greaterThan = Instant.now().minus(24, ChronoUnit.HOURS).minusSeconds(10);
        ArgumentCaptor<Instant> argument = ArgumentCaptor.forClass(Instant.class);
        verify(envelopeRepository)
            .findByJurisdictionAndStatusAndCreatedAtGreaterThan(
                eq("testJurisdiction"),
                eq(UPLOADED),
                argument.capture()
            );
        var time = argument.getValue();
        assertThat(time).isAfter(greaterThan).isBefore(Instant.now());
    }

    @Test
    void should_return_all_envelopes_if_status_is_null() throws Exception {
        // given
        List<Envelope> envelopes =
            asList(
                envelope(),
                envelope(),
                envelope()
            );

        List<EnvelopeResponse> envelopesResponse = EnvelopeResponseMapper.toEnvelopesResponse(envelopes);

        given(envelopeAccess.getReadJurisdictionForService("testService"))
            .willReturn("testJurisdiction");

        given(envelopeRepository.findByJurisdictionAndCreatedAtGreaterThan(eq("testJurisdiction"), any()))
            .willReturn(envelopes);

        // when
        List<EnvelopeResponse> foundEnvelopes = envelopeRetrieverService
            .findByServiceAndStatus("testService", null);

        // then
        assertThat(foundEnvelopes)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(envelopesResponse);
    }

    @Test
    void should_throw_data_retrieval_failure_exception_when_repository_fails_to_retrieve_envelopes() {
        when(envelopeAccess.getReadJurisdictionForService("testService"))
            .thenReturn("testJurisdiction");

        when(envelopeRepository
                 .findByJurisdictionAndStatusAndCreatedAtGreaterThan(eq("testJurisdiction"), eq(UPLOADED), any()))
            .thenThrow(DataRetrievalFailureException.class);

        Throwable throwable = catchThrowable(() ->
            envelopeRetrieverService.findByServiceAndStatus("testService", UPLOADED));

        assertThat(throwable).isInstanceOf(DataRetrievalFailureException.class);
    }

    @Test
    void should_throw_envelope_not_found_exception_when_there_is_no_envelope() {
        given(envelopeRepository
                  .findFirstByZipFileNameAndContainerOrderByCreatedAtDesc(
                      "a.zip",
                      "Container_A"
                  ))
            .willReturn(null);

        Throwable throwable =
            catchThrowable(() ->
                               envelopeRetrieverService
                                   .findByFileNameAndContainer("a.zip", "Container_A")
            );

        assertThat(throwable).isInstanceOf(EnvelopeNotFoundException.class);
    }

    @Test
    void should_return_envelope_when_there_is_matching_envelope() {

        var envelope = envelope();
        var expectedResponse = EnvelopeResponseMapper.toEnvelopeResponse(envelope);

        given(envelopeRepository
                  .findFirstByZipFileNameAndContainerOrderByCreatedAtDesc(
                      "a.zip",
                      "Container_A"
                  ))
            .willReturn(envelope);

        var result = envelopeRetrieverService
            .findByFileNameAndContainer("a.zip", "Container_A");

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResponse);
    }
}
