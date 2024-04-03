package uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly;

import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.stereotype.Service;

/**
 * Factory for creating LaunchDarkly clients.
 */
@Service
public class LaunchDarklyClientFactory {
    /**
     * Creates a new instance of the client.
     * @param sdkKey the SDK key
     * @param offlineMode whether to use offline mode
     * @return the client
     */
    public LDClientInterface create(String sdkKey, boolean offlineMode) {
        LDConfig config = new LDConfig.Builder()
            .offline(offlineMode)
            .build();
        return new LDClient(sdkKey, config);
    }
}
