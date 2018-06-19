package uk.gov.hmcts.reform.bulkscanning.services;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.core.PathUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanning.config.AccessTokenConfigurationProperties;
import uk.gov.hmcts.reform.bulkscanning.exceptions.ServiceConfigNotFoundException;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class SasTokenGeneratorServiceTest {

    private AccessTokenConfigurationProperties accessTokenConfigurationProperties;
    private SasTokenGeneratorService tokenGeneratorService;

    @Before
    public void setUp() throws URISyntaxException {
        StorageCredentials storageCredentials = new StorageCredentialsAccountAndKey("testAccountName", "dGVzdGtleQ==");

        CloudBlobClient cloudBlobClient = new CloudStorageAccount(storageCredentials, true).createCloudBlobClient();

        createAccessTokenConfig();

        tokenGeneratorService = new SasTokenGeneratorService(
            cloudBlobClient,
            storageCredentials,
            accessTokenConfigurationProperties
        );
    }

    @Test
    public void should_generate_sas_token_when_service_configuration_is_available() throws StorageException {
        String sasToken = tokenGeneratorService.generateSasToken("sscs");

        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Map<String, String[]> queryParams = PathUtility.parseQueryString(sasToken);

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
        assertThat(queryParams.get("se")[0]).startsWith(currentDate);//the expiry date/time for the signature
        assertThat(queryParams.get("sv")).contains("2017-07-29");//azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl");//access permissions(write-w,list-l)
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("doesnotexist"))
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for service doesnotexist");
    }

    private void createAccessTokenConfig() {
        AccessTokenConfigurationProperties.TokenConfig tokenConfig = new AccessTokenConfigurationProperties.TokenConfig();
        tokenConfig.setPermissions("WRITE,LIST");
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenConfigurationProperties = new AccessTokenConfigurationProperties();
        accessTokenConfigurationProperties.setServiceConfig(singletonList(tokenConfig));
    }
}
