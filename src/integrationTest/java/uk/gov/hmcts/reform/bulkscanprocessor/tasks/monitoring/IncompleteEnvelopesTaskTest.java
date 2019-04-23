package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@IntegrationTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "monitoring.incomplete-envelopes.enabled=true"
})
@Import(IncompleteEnvelopesTaskTest.SpyOnJpa.class)
public class IncompleteEnvelopesTaskTest {

    @Autowired
    private EnvelopeRepository repository;

    @Autowired
    private IncompleteEnvelopesTask task;

    // only verifying integrity as validity is covered with unit test
    // no dependencies involved - just log messages
    // checking output is not recommended especially after moving to next gen of java-logging library
    @Test
    public void should_call_repository_method_and_not_fail_the_task() {
        assertThatCode(task::run).doesNotThrowAnyException();

        verify(repository).getIncompleteEnvelopesCountBefore(LocalDate.now());
    }

    @TestConfiguration
    static class SpyOnJpa {

        @Bean
        @Primary
        public EnvelopeRepository getEnvelopeRepositorySpy(final EnvelopeRepository originalEnvelopeRepository) {
            return mock(EnvelopeRepository.class, delegatesTo(originalEnvelopeRepository));
        }
    }
}
