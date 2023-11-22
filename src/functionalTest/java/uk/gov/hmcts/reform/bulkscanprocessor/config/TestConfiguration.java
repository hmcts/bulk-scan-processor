package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.yaml")
public final class TestConfiguration {

    private TestConfiguration() {
    }

    @Value("${test-url}")
    public static String TEST_URL;

    @Value("${test-scan-delay}")
    public static long SCAN_DELAY;

    @Value("${test-s2s-url}")
    public static String S2S_URL;

    @Value("${test-s2s-name}")
    public static String S2S_NAME;

    @Value("${test-s2s-secret}")
    public static String S2S_SECRET;

    @Value("${flux-func-test}")
    public static boolean FLUX_FUNC_TEST;

    @Value("${test-storage-account-url}")
    public static String STORAGE_ACCOUNT_URL;

    @Value("${test-storage-account-name}")
    public static String STORAGE_ACCOUNT_NAME;

    @Value("${test-storage-account-key}")
    public static String STORAGE_ACCOUNT_KEY;

    @Value("${test-storage-container-name}")
    public static String STORAGE_CONTAINER_NAME;

    @Value("${processed-envelopes-queue-namespace}")
    public static String PROCESSED_ENVELOPES_QUEUE_NAMESPACE;

    @Value("${processed-envelopes-queue-access-key-name}")
    public static String PROCESSED_ENVELOPES_QUEUE_ACCESS_KEY_NAME;

    @Value("${processed-envelopes-queue-access-key}")
    public static String PROCESSED_ENVELOPES_QUEUE_ACCESS_KEY;

    public static String PROCESSED_ENVELOPES_QUEUE_CONN_STRING = String.format(
        "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;",
        PROCESSED_ENVELOPES_QUEUE_NAMESPACE,
        PROCESSED_ENVELOPES_QUEUE_ACCESS_KEY_NAME,
        PROCESSED_ENVELOPES_QUEUE_ACCESS_KEY
    );

    @Value("${processed-envelopes-queue-name}")
    public static String PROCESSED_ENVELOPES_QUEUE_NAME;

    @Value("${jms-enabled}")
    public static boolean JMS_ENABLED;
}
