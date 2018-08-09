package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import com.google.common.collect.ImmutableList;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class EnvelopeCreator {

    private static final EnvelopeResponseMapper mapper = new EnvelopeResponseMapper();


    private EnvelopeCreator() {
    }

    public static List<EnvelopeResponse> envelopeResponses() throws Exception {
        return mapper.toEnvelopesResponse(envelopes());
    }

    public static EnvelopeResponse envelopeResponse() throws Exception {
        return mapper.toEnvelopeResponse(envelope());
    }

    public static List<Envelope> envelopes() throws Exception {
        return ImmutableList.of(envelope());
    }

    public static Envelope envelope() throws Exception {
        return envelope("SSCS", Status.PROCESSED);
    }

    public static Envelope envelope(String jurisdiction, Status status) throws Exception {
        Timestamp timestamp = getTimestamp();

        Envelope envelope = new Envelope(
            "SSCSPO",
            jurisdiction,
            timestamp,
            timestamp,
            timestamp,
            UUID.randomUUID() + ".zip",
            null,
            null,
            scannableItems(),
            payments(),
            nonScannableItems()
        );

        envelope.setStatus(status);
        envelope.setContainer("SSCS");

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
            "test",
            null
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
