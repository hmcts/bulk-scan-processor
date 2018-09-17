package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidStatusChangeException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

public class EnvelopeStatusChangeValidatorTest {

    private EnvelopeStatusChangeValidator validator = new EnvelopeStatusChangeValidator();

    @Test
    public void should_throw_an_exception_if_status_transition_is_not_allowed() {
        Status from = CONSUMED;
        Status to = UPLOAD_FAILURE;

        assertThatThrownBy(() -> validator.assertCanUpdate(from, to))
            .isInstanceOf(InvalidStatusChangeException.class)
            .hasMessageContaining(from.name())
            .hasMessageContaining(to.name());
    }

    @Test
    public void should_not_throw_an_exception_if_status_transition_is_allowed() {
        assertThatCode(() -> validator.assertCanUpdate(NOTIFICATION_SENT, CONSUMED))
            .doesNotThrowAnyException();
    }
}
