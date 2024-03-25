package uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Client for LaunchDarkly feature flags.
 */
@Service
public class LaunchDarklyClient {

    private final LDContext bulkScanProcessorContext;
    private final LDClientInterface internalClient;

    /**
     * Creates a new instance of the client.
     * @param launchDarklyClientFactory the factory for creating the internal client
     * @param sdkKey the SDK key
     * @param offlineMode whether to use offline mode
     */
    @Autowired
    public LaunchDarklyClient(
        LaunchDarklyClientFactory launchDarklyClientFactory,
        @Value("${launchdarkly.sdk-key:YYYYY}") String sdkKey,
        @Value("${launchdarkly.offline-mode:false}") Boolean offlineMode
    ) {
        this.internalClient = launchDarklyClientFactory.create(sdkKey, offlineMode);
        this.bulkScanProcessorContext = LDContext.builder(sdkKey).build();
    }

    /**
     * Checks if a feature is enabled.
     * @param feature the feature name
     * @return true if the feature is enabled
     */
    public boolean isFeatureEnabled(String feature) {
        internalClient.flush();
        System.gc();
        return internalClient.boolVariation(feature, bulkScanProcessorContext, false);
    }

    /**
     * Checks if a feature is enabled.
     * @param feature the feature name
     * @param context the context
     * @return true if the feature is enabled
     */
    public boolean isFeatureEnabled(String feature, LDContext context) {
        internalClient.flush();
        System.gc();
        return internalClient.boolVariation(feature, context, false);
    }

    /**
     * Gets the status of the data source.
     * @return the status
     */
    public DataSourceStatusProvider.Status getDataSourceStatus() {
        return internalClient.getDataSourceStatusProvider().getStatus();
    }
}
