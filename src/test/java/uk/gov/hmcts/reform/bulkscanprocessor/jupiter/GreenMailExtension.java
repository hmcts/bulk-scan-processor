package uk.gov.hmcts.reform.bulkscanprocessor.jupiter;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailProxy;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This is a copy of not yet released code.
 * {@see https://github.com/greenmail-mail-test/greenmail/tree/master/greenmail-junit5/src/main/java/com/icegreen/greenmail/junit5}
 */
public class GreenMailExtension extends GreenMailProxy implements BeforeEachCallback, AfterEachCallback {
    private final ServerSetup[] serverSetups;
    private GreenMail greenMail;

    /**
     * Initialize with single server setups.
     *
     * @param serverSetup Setup to use
     */
    public GreenMailExtension(final ServerSetup serverSetup) {
        this.serverSetups = new ServerSetup[]{serverSetup};
    }

    /**
     * Initialize with all server setups.
     */
    public GreenMailExtension() {
        this(ServerSetupTest.ALL);
    }

    /**
     * Initialize with multiple server setups.
     *
     * @param serverSetups All setups to use
     */
    public GreenMailExtension(final ServerSetup[] serverSetups) {
        this.serverSetups = serverSetups;
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        greenMail = new GreenMail(serverSetups);
        start();
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        stop();
    }

    @Override
    protected GreenMail getGreenMail() {
        return greenMail;
    }

    @Override
    public GreenMailExtension withConfiguration(final GreenMailConfiguration config) {
        super.withConfiguration(config);
        return this;
    }
}
