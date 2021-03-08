package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeInfo;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@ExtendWith(MockitoExtension.class)
class IncompleteEnvelopesServiceTest {
    private IncompleteEnvelopesService incompleteEnvelopesService;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @BeforeEach
    void setUp() {
        incompleteEnvelopesService = new IncompleteEnvelopesService(envelopeRepository);
    }

    @Test
    void should_propagate_result_from_repository() {
        // given
        List<Envelope> envelopes = asList(
            envelope("file1.zip", "CMC", COMPLETED, emptyList(), "cmc"),
            envelope("file2.zip", "SSCS", COMPLETED, emptyList(), "sscs")
        );
        given(envelopeRepository.getIncompleteEnvelopesBefore(any(LocalDateTime.class)))
            .willReturn(envelopes);

        // when
        List<EnvelopeInfo> result = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        // then
        assertThat(result)
            .extracting(e -> tuple(e.fileName, e.container))
            .containsExactlyInAnyOrder(
                tuple("file1.zip", "cmc"),
                tuple("file2.zip", "sscs")
            )
        ;
    }

    @Test
    void should_propagate_empty_result() {
        // given
        given(envelopeRepository.getIncompleteEnvelopesBefore(any(LocalDateTime.class)))
            .willReturn(emptyList());

        // when
        List<EnvelopeInfo> result = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        // then
        assertThat(result).isEmpty();
    }
}
