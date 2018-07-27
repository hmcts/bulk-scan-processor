package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import com.google.common.collect.ImmutableList;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class EnvelopeCreator {

    private EnvelopeCreator() {
    }

    public static List<Envelope> envelopes() throws Exception {
        return ImmutableList.of(envelope());
    }

    public static Envelope envelope() throws Exception {
        Timestamp timestamp = getTimestamp();

        Envelope envelope = new Envelope(
            "SSCSPO",
            "SSCS",
            timestamp,
            timestamp,
            timestamp,
            "7_24-06-2018-00-00-00.zip",
            scannableItems(),
            payments(),
            nonScannableItems()
        );

        envelope.setStatus(Event.DOC_PROCESSED);

        return envelope;
    }

    private static List<ScannableItem> scannableItems() throws Exception {
        Timestamp timestamp = getTimestamp();


        ScannableItem scannableItem = new ScannableItem(
            "1111002",
            timestamp,
            "test",
            "test",
            "return",
            timestamp,
            "dGVzdA==", //Base 64 value=test
            "1111002.pdf",
            "test"
        );
        scannableItem.setDocumentUrl("http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe");

        return ImmutableList.of(scannableItem);
    }

    private static List<NonScannableItem> nonScannableItems() {
        return ImmutableList.of(
            new NonScannableItem("CD", "4GB USB memory stick")
        );
    }

    private static List<Payment> payments() {
        return ImmutableList.of(
            new Payment("1111002", "Cheque", "100.00", "GBP")
        );
    }

    private static Timestamp getTimestamp() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date date = dateFormat.parse("23-06-2018");
        long time = date.getTime();
        return new Timestamp(time);
    }
}
