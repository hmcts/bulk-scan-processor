package uk.gov.hmcts.reform.bulkscanprocessor.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.AuthenticationChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.JurisdictionConfigurationStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IdamHealthIndicatorTest {

    @Mock
    private AuthenticationChecker authenticationChecker;

    @Test
    void should_return_up_when_all_configured_idam_users_can_authenticate() {
        given(authenticationChecker.checkSignInForAllJurisdictions()).willReturn(List.of(
            new JurisdictionConfigurationStatus("bulkscan", true)
        ));

        Health health = new IdamHealthIndicator(authenticationChecker).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void should_return_down_when_a_configured_idam_user_cannot_authenticate() {
        given(authenticationChecker.checkSignInForAllJurisdictions()).willReturn(List.of(
            new JurisdictionConfigurationStatus("bulkscan", true),
            new JurisdictionConfigurationStatus("sscs", false)
        ));

        Health health = new IdamHealthIndicator(authenticationChecker).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
