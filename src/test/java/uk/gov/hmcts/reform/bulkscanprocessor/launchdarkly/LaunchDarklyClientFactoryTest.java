package uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly;

import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class LaunchDarklyClientFactoryTest {
    private LaunchDarklyClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LaunchDarklyClientFactory();
    }

    @Disabled
    @Test
    void testCreate() throws IOException {
        try (LDClientInterface client = factory.create("test key", true)) {
            assertThat(client).isNotNull();
        }
    }
}
