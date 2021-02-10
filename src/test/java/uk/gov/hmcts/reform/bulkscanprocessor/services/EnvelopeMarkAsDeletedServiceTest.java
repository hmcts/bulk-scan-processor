package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeJdbcRepository;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnvelopeMarkAsDeletedServiceTest {
    private EnvelopeMarkAsDeletedService envelopeMarkAsDeletedService;

    @Mock
    private EnvelopeJdbcRepository envelopeJdbcRepository;

    @BeforeEach
    void setUp() {
        envelopeMarkAsDeletedService = new EnvelopeMarkAsDeletedService(envelopeJdbcRepository);
    }

    @Test
    void should_call_repository() {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        envelopeMarkAsDeletedService.markEnvelopeAsDeleted(envelopeId, "loggingContext");

        // then
        verify(envelopeJdbcRepository).markEnvelopeAsDeleted(envelopeId);
    }
}
