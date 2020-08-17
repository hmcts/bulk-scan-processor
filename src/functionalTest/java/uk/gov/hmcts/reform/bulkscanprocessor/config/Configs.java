package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Configs {

    private static final Config config;

    public static final String TEST_URL;

    public static final long SCAN_DELAY;

    public static final String S2S_URL;

    public static final String S2S_NAME;

    public static final String S2S_SECRET;

    public static final String PROXY_HOST;

    public static final String PROXY_PORT;

    public static final boolean IS_PROXY_ENABLED;

    public static final boolean FLUX_FUNC_TEST;

    public static final String STORAGE_ACCOUNT_URL;

    public static final String STORAGE_ACCOUNT_NAME;

    public static final String STORAGE_ACCOUNT_KEY;

    public static final String STORAGE_CONTAINER_NAME;

    public static final String PROCESSED_ENVELOPES_QUEUE_CONN_STRING;

    static {
       config = ConfigFactory.load();
       TEST_URL = config.getString("test-url");
       SCAN_DELAY = Long.parseLong(config.getString("test-scan-delay"));
       S2S_URL = config.getString("test-s2s-url");
       S2S_NAME = config.getString("test-s2s-name");
       S2S_SECRET = config.getString("test-s2s-secret");
       PROXY_HOST = config.getString("storage-proxy-host");
       PROXY_PORT = config.getString("storage-proxy-port");
       IS_PROXY_ENABLED = Boolean.valueOf(config.getString("proxyout.enabled"));
       FLUX_FUNC_TEST = config.getBoolean("flux-func-test");
       STORAGE_ACCOUNT_URL = config.getString("test-storage-account-url");
       STORAGE_ACCOUNT_NAME = config.getString("test-storage-account-name");
       STORAGE_ACCOUNT_KEY = config.getString("test-storage-account-key");
       STORAGE_CONTAINER_NAME = config.getString("test-storage-container-name");
       PROCESSED_ENVELOPES_QUEUE_CONN_STRING =  config.getString("processed-envelopes-queue-conn-string");
   }

    private Configs() {
    }
}
