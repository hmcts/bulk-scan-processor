package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class SasTokenGeneratorServiceTest {

    private SasTokenGeneratorService tokenGeneratorService;

    @Before
    public void setUp() throws InvalidKeyException {
        SharedKeyCredentials credentials = new SharedKeyCredentials("testAccountName", "dGVzdGtleQ==");
        AccessTokenProperties accessTokenConfig = createAccessTokenConfig();

        tokenGeneratorService = new SasTokenGeneratorService(
            credentials,
            accessTokenConfig
        );
    }

    @Test
    public void should_generate_sas_token_when_service_configuration_is_available() throws StorageException {
        String sasToken = tokenGeneratorService.generateSasToken("sscs");

        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Map<String, List<String>> queryParams = UriComponentsBuilder.fromUriString(sasToken).build()
            .getQueryParams();

        assertThat(queryParams.get("sig").get(0)).isNotNull(); //this is a generated hash of the resource string
        assertThat(queryParams.get("se").get(0)).startsWith(currentDate); //the expiry date/time for the signature
        assertThat(queryParams.get("sv").get(0)).isEqualTo("2018-03-28"); //azure api version is latest
        assertThat(queryParams.get("sp").get(0)).isEqualTo("wl"); //access permissions(write-w,list-l)
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("doesnotexist"))
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for service doesnotexist");
    }

    private AccessTokenProperties createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        AccessTokenProperties accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));

        return accessTokenProperties;
    }
}
