package uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LaunchDarklyClient {

    private final LDContext bulkScanProcessorContext;
    private final LDClientInterface internalClient;

    @Autowired
    public LaunchDarklyClient(
        LaunchDarklyClientFactory launchDarklyClientFactory,
        @Value("${launchdarkly.sdk-key:YYYYY}") String sdkKey,
        @Value("${launchdarkly.offline-mode:false}") Boolean offlineMode
    ) {
        this.internalClient = launchDarklyClientFactory.create(sdkKey, offlineMode);
        this.bulkScanProcessorContext = LDContext.builder(sdkKey).build();
    }

    public boolean isFeatureEnabled(String feature) {
        internalClient.flush();
        System.gc();
        return internalClient.boolVariation(feature, bulkScanProcessorContext, false);
    }

    public boolean isFeatureEnabled(String feature, LDContext context) {
        internalClient.flush();
        System.gc();
        return internalClient.boolVariation(feature, context, false);
    }

    public DataSourceStatusProvider.Status getDataSourceStatus() {
        return internalClient.getDataSourceStatusProvider().getStatus();
    }
}
