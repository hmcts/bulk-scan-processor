package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestConfiguration {

    private static final Config config;

    public static final String TEST_URL;

    public static final long SCAN_DELAY;

    public static final String S2S_URL;

    public static final String S2S_NAME;

    public static final String S2S_SECRET;

    public static final boolean FLUX_FUNC_TEST;

    public static final String STORAGE_ACCOUNT_URL;

    public static final String STORAGE_ACCOUNT_NAME;

    public static final String STORAGE_ACCOUNT_KEY;

    public static final String STORAGE_CONTAINER_NAME;

    public static final String PROCESSED_ENVELOPES_QUEUE_CONN_STRING;

    public static final String PROCESSED_ENVELOPES_QUEUE_NAME;

    public static final boolean JMS_ENABLED;

    static {
        config = ConfigFactory.load();
        TEST_URL = config.getString("test-url");
        SCAN_DELAY = Long.parseLong(config.getString("test-scan-delay"));
        S2S_URL = config.getString("test-s2s-url");
        S2S_NAME = config.getString("test-s2s-name");
        S2S_SECRET = config.getString("test-s2s-secret");
        FLUX_FUNC_TEST = config.getBoolean("flux-func-test");
        STORAGE_ACCOUNT_URL = config.getString("test-storage-account-url");
        STORAGE_ACCOUNT_NAME = config.getString("test-storage-account-name");
        STORAGE_ACCOUNT_KEY = config.getString("test-storage-account-key");
        STORAGE_CONTAINER_NAME = config.getString("test-storage-container-name");
        JMS_ENABLED = config.getBoolean("jms-enabled");

        PROCESSED_ENVELOPES_QUEUE_CONN_STRING = String.format(
            "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;",
            config.getString("processed-envelopes-queue-namespace"),
            config.getString("processed-envelopes-queue-access-key-name"),
            config.getString("processed-envelopes-queue-access-key")
        );
        PROCESSED_ENVELOPES_QUEUE_NAME = config.getString("processed-envelopes-queue-name");
    }

    private TestConfiguration() {
    }
}
