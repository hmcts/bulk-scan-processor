package uk.gov.hmcts.reform.bulkscanprocessor;

import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly.LaunchDarklyClient;
import uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly.LaunchDarklyClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly.Flags.BULK_SCAN_PROCESSOR_TEST;

@TestPropertySource("classpath:application.yaml")
@ExtendWith(SpringExtension.class)
class TestLaunchDarkly {

    @Value("${sdk-key:YYYYY}")
    private String sdkKey;

    @Value("${offline-mode:false}")
    private Boolean offlineMode;

    private LaunchDarklyClient ldClient;

    @BeforeEach
    void setUp() {
        LaunchDarklyClientFactory ldFactory = new LaunchDarklyClientFactory();
        ldClient = new LaunchDarklyClient(ldFactory, sdkKey, offlineMode);
    }

    @Test
    void checkLaunchDarklyStatus() throws InterruptedException {

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 60_000; // One minute in milliseconds

        DataSourceStatusProvider.Status ldStatus;

        do {
            ldStatus = ldClient.getDataSourceStatus();
            if (ldStatus.getState() == DataSourceStatusProvider.State.VALID) {
                break; // Exit the loop if status is VALID
            }
            Thread.sleep(5_000); // Wait 5 seconds before polling again
        } while (System.currentTimeMillis() < endTime);

        assertThat(ldStatus.getState()).isEqualTo(DataSourceStatusProvider.State.VALID);
    }

    @Test
    void checkLaunchDarklyTestFlag() {
        Boolean testFeatureBoolean = ldClient.isFeatureEnabled(BULK_SCAN_PROCESSOR_TEST);
        assertThat(testFeatureBoolean).isTrue();
        //BULK_SCAN_PROCESSOR_TEST is a test flag only and needs to be set to TRUE within LD.
    }
}
