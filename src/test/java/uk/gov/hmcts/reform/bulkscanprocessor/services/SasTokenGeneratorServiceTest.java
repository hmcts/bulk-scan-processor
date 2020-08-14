package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.core.PathUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class SasTokenGeneratorServiceTest {

    private AccessTokenProperties accessTokenProperties;
    private SasTokenGeneratorService tokenGeneratorService;

    @BeforeEach
    public void setUp() {
        StorageSharedKeyCredential storageCredentials =
            new StorageSharedKeyCredential("testAccountName", "dGVzdGtleQ==");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .credential(storageCredentials)
            .endpoint("http://test.account")
            .buildClient();

        createAccessTokenConfig();

        tokenGeneratorService = new SasTokenGeneratorService(
            blobServiceClient,
            accessTokenProperties
        );
    }

    @Test
    public void should_generate_sas_token_when_service_configuration_is_available() throws StorageException {
        String sasToken = tokenGeneratorService.generateSasToken("sscs");

        String currentDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(OffsetDateTime.now(UTC));

        Map<String, String[]> queryParams = PathUtility.parseQueryString(sasToken);

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
        assertThat(queryParams.get("se")[0]).startsWith(currentDate);//the expiry date/time for the signature
        assertThat(queryParams.get("sv")).contains("2019-07-07");//azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl");//access permissions(write-w,list-l)
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        assertThatThrownBy(() -> tokenGeneratorService.generateSasToken("doesnotexist"))
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessage("No service configuration found for service doesnotexist");
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}
