package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import com.google.common.collect.ImmutableList;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public final class EnvelopeCreator {
    private static final Timestamp CURRENT_TIMESTAMP = new Timestamp(System.currentTimeMillis());

    private EnvelopeCreator() {
    }

    public static List<Envelope> envelopes() {
        return ImmutableList.of(
            new Envelope(
                "sscpo",
                "sscs",
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                "test.zip",
                scannableItems(),
                payments(),
                nonScannableItems()
            )
        );
    }

    private static List<ScannableItem> scannableItems() {
        return ImmutableList.of(
            new ScannableItem(
                "11110",
                CURRENT_TIMESTAMP,
                "test",
                "test",
                "test",
                CURRENT_TIMESTAMP,
                "dGVzdA==", //Base 64 value=test
                "11110.pdf",
                "test"
            )
        );
    }

    private static List<NonScannableItem> nonScannableItems() {
        return ImmutableList.of(
            new NonScannableItem("CD", "test")
        );
    }

    private static List<Payment> payments() {
        return ImmutableList.of(
            new Payment("11110", "Cheque", "100", "GBP")
        );
    }
}
