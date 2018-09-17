package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class EnvelopeCreator {

    private static final MetafileJsonValidator validator;

    static {
        try {
            validator = new MetafileJsonValidator();
        } catch (IOException | ProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    private EnvelopeCreator() {
    }

    public static InputStream getMetaFile() {
        return EnvelopeCreator.class.getResourceAsStream("/metafile.json");
    }

    public static Envelope getEnvelopeFromMetafile() throws IOException {
        try (InputStream inputStream = getMetaFile()) {
            return validator.parseMetafile(IOUtils.toByteArray(inputStream));
        }
    }

    public static Envelope getEnvelopeFromMetafile(byte[] metafile) throws IOException {
        return validator.parseMetafile(metafile);
    }

    public static List<EnvelopeResponse> envelopeResponses() throws Exception {
        return EnvelopeResponseMapper.toEnvelopesResponse(envelopes());
    }

    public static EnvelopeResponse envelopeResponse() throws Exception {
        return EnvelopeResponseMapper.toEnvelopeResponse(envelope());
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
            "1111222233334446",
            Classification.EXCEPTION,
            false,
            scannableItems(),
            payments(),
            nonScannableItems()
        );

        envelope.setStatus(status);
        envelope.setContainer("SSCS");

        return envelope;
    }

    public static Envelope envelopeNotified() throws Exception {
        return envelope("SSCS", Status.NOTIFICATION_SENT);
    }

    public static Envelope envelopeUrgent() throws Exception {
        return envelopeUrgent("SSCS", Status.CREATED);
    }

    public static Envelope envelopeUrgent(String jurisdiction, Status status) throws Exception {
        Timestamp timestamp = getTimestamp();

        Envelope envelope = new Envelope(
            "SSCSPO",
            jurisdiction,
            timestamp,
            timestamp,
            timestamp,
            UUID.randomUUID() + ".zip",
            "1111222233334446",
            Classification.NEW_APPLICATION,
            true,
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

        ScannableItem scannableItem1 = new ScannableItem(
            "1111001",
            timestamp,
            "test",
            "test",
            "return",
            timestamp,
            "dGVzdA==", //Base 64 value=test
            "1111001.pdf",
            "test",
            "Other"
        );
        scannableItem1.setDocumentUrl("http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebceb");


        ScannableItem scannableItem2 = new ScannableItem(
            "1111002",
            timestamp,
            "test",
            "test",
            "return",
            timestamp,
            "dGVzdA==", //Base 64 value=test
            "1111002.pdf",
            "test",
            "Other"
        );
        scannableItem2.setDocumentUrl("http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe");

        return ImmutableList.of(scannableItem1, scannableItem2);
    }

    private static List<NonScannableItem> nonScannableItems() {
        return ImmutableList.of(
            new NonScannableItem("1111001", "CD", "4GB USB memory stick")
        );
    }

    private static List<Payment> payments() {
        return ImmutableList.of(
            new Payment("1111002", "Cheque", "100.00", "GBP", "1000000000", "112233", "12345678")
        );
    }

    private static Timestamp getTimestamp() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date date = dateFormat.parse("23-06-2018");
        long time = date.getTime();
        return new Timestamp(time);
    }
}
