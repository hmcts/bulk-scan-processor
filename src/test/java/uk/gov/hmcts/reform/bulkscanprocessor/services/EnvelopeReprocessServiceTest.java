package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EnvelopeReprocessServiceTest {
    private EnvelopeReprocessService envelopeReprocessService;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    @BeforeEach
    void setUp() {
        envelopeReprocessService = new EnvelopeReprocessService(
            envelopeRepository,
            processEventRepository
        );
    }

    @Test
    void should_throw_exception_if_envelope_does_not_exist() {
        // given
        UUID uuid = UUID.randomUUID();

        given(envelopeRepository.findById(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() ->
            envelopeReprocessService.reprocessEnvelope(uuid.toString())
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid.toString() + " not found");
    }
}
