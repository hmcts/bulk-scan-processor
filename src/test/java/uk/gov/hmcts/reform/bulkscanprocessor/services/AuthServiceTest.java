package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    private static final String SERVICE_HEADER = "some-header";

    @Mock
    private AuthTokenValidator validator;

    private AuthService service;

    @BeforeEach
    public void setUp() {
        service = new AuthService(validator);
    }

    @AfterEach
    public void tearDown() {
        reset(validator);
    }

    @Test
    public void should_throw_missing_header_exception_when_it_is_null() {
        // when
        Throwable exception = catchThrowable(() -> service.authenticate(null));

        // then
        assertThat(exception)
            .isInstanceOf(UnAuthenticatedException.class)
            .hasMessage("Missing ServiceAuthorization header");

        // and
        verify(validator, never()).getServiceName(anyString());
    }

    @Test
    public void should_track_failure_in_service_dependency_when_invalid_token_received() {
        // given
        willThrow(InvalidTokenException.class).given(validator).getServiceName(anyString());

        // when
        Throwable exception = catchThrowable(() -> service.authenticate(SERVICE_HEADER));

        // then
        assertThat(exception).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void should_track_successful_service_dependency_when_valid_token_received() {
        // given
        given(validator.getServiceName(SERVICE_HEADER)).willReturn("some-service");

        // when
        String serviceName = service.authenticate(SERVICE_HEADER);

        // then
        assertThat(serviceName).isEqualTo("some-service");
    }
}
