package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoUserConfiguredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(SpringExtension.class)
@Import(JurisdictionToUserMapping.class)
@EnableConfigurationProperties
@TestPropertySource(properties = {
    "idam.users.sscs.username=user@example.com",
    "idam.users.sscs.password=password"
})
class JurisdictionToUserMappingTest {

    @Autowired
    JurisdictionToUserMapping mapping;

    @Test
    void should_parse_up_the_properties_into_map() {
        Credential creds = mapping.getUser("SSCS");
        assertThat(creds.getPassword()).isEqualTo("password");
        assertThat(creds.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    void should_throw_exception_if_not_found() {
        Throwable throwable = catchThrowable(() -> mapping.getUser("NONE"));

        assertThat(throwable)
            .isInstanceOf(NoUserConfiguredException.class)
            .hasMessage("No user configured for jurisdiction: none");
    }

    @Test
    void should_throw_exception_if_none_configured() {
        Throwable throwable = catchThrowable(() -> new JurisdictionToUserMapping().getUser("NONE"));
        assertThat(throwable)
            .isInstanceOf(NoUserConfiguredException.class)
            .hasMessage("No user configured for jurisdiction: none");
    }
}
