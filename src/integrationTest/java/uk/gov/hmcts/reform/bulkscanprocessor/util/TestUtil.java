package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class TestUtil {

    private TestUtil() {
    }

    public static String fileContentAsString(String file) throws IOException {
        return new String(fileContentAsBytes(file), StandardCharsets.UTF_8);
    }

    public static byte[] fileContentAsBytes(String file) throws IOException {
        return Resources.toByteArray(Resources.getResource(file));
    }
}
