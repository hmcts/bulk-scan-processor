package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;

@RunWith(MockitoJUnitRunner.class)
public class OcrValidatorTest {

    @Mock private OcrValidationClient client;
    @Mock private ContainerMappings containerMappings;
    @Mock private AuthTokenGenerator authTokenGenerator;

    @Captor
    private ArgumentCaptor<FormData> argCaptor;

    private OcrValidator ocrValidator;

    private static final String S2S_TOKEN = "sample-s2s-token";
    private static final String PO_BOX = "sample PO box";

    @Before
    public void setUp() throws Exception {
        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        this.ocrValidator = new OcrValidator(client, containerMappings, authTokenGenerator);
    }

    @Test
    public void should_call_rest_client_with_correct_parameters() {
        // given
        String url = "https://example.com/validate-ocr";
        String subtype = "sample_document_subtype";
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            PO_BOX,
            Classification.EXCEPTION,
            asList(
                doc(subtype, sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, url)
            ));

        given(client.validate(eq(url), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        // when
        ocrValidator.assertIsValid(envelope);

        // then
        verify(client).validate(eq(url), argCaptor.capture(), eq(S2S_TOKEN));
        assertThat(argCaptor.getValue().type).isEqualTo(subtype);
        assertThat(argCaptor.getValue().ocrDataFields)
            .extracting(it -> tuple(it.name, it.value))
            .containsExactlyElementsOf(
                sampleOcr()
                    .getFields()
                    .stream()
                    .map(it -> tuple(it.name.textValue(), it.value.textValue()))
                    .collect(toList())
            );
    }

    @Test
    public void should_not_call_validation_if_url_is_not_configured() {
        // given
        InputEnvelope envelope = envelope(
            "samplePoBox",
            asList(
                doc("D8", sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings()).willReturn(emptyList()); // url not configured

        // when
        ocrValidator.assertIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any());
    }

    @Test
    public void should_not_call_validation_there_are_no_documents_with_ocr() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("other", null),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", envelope.poBox, "https://example.com")
            ));

        // when
        ocrValidator.assertIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any());
    }

    @Test
    public void should_throw_an_exception_if_service_responded_with_error_response() {
        // given
        String url = "https://example.com/validate-ocr";
        String subtype = "sample_document_subtype";
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc(subtype, sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, url)
            ));

        given(client.validate(eq(url), any(), any()))
            .willReturn(new ValidationResponse(Status.ERRORS, emptyList(), singletonList("Error!")));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // when
        Throwable err = catchThrowable(() -> ocrValidator.assertIsValid(envelope));


        // then
        assertThat(err)
            .isInstanceOf(OcrValidationException.class)
            .hasMessageContaining("Error!");
    }

    @Test
    public void should_continue_if_calling_validation_endpoint_fails() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("form", sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", envelope.poBox, "https://example.com")
            ));

        given(client.validate(any(), any(), any())).willThrow(new RuntimeException());

        // when
        Throwable err = catchThrowable(() -> ocrValidator.assertIsValid(envelope));

        // then
        assertThat(err).isNull();
        verify(client).validate(any(), any(), any());
    }

    private InputOcrData sampleOcr() {
        InputOcrData data = new InputOcrData();
        data.setFields(asList(
            new InputOcrDataField(new TextNode("hello"), new TextNode("world")),
            new InputOcrDataField(new TextNode("foo"), new TextNode("bar"))
        ));
        return data;
    }

    private InputEnvelope envelope(String poBox, List<InputScannableItem> scannableItems) {
        return inputEnvelope(
            "BULKSCAN",
            poBox,
            Classification.EXCEPTION,
            scannableItems
        );
    }

    private InputScannableItem doc(String subtype, InputOcrData ocrData) {
        return new InputScannableItem(
            UUID.randomUUID().toString(),
            Instant.now(),
            null,
            null,
            null,
            null,
            ocrData,
            null,
            null,
            null,
            subtype
        );
    }
}
