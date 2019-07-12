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
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;

@RunWith(MockitoJUnitRunner.class)
public class OcrValidatorTest {

    @Mock private OcrValidationClient client;
    @Mock private ContainerMappings containerMappings;
    @Mock private AuthTokenGenerator authTokenGenerator;

    @Captor ArgumentCaptor<FormData> argCaptor;

    private OcrValidator ocrValidator;

    @Before
    public void setUp() throws Exception {
        this.ocrValidator = new OcrValidator(client, containerMappings, authTokenGenerator);
    }

    @Test
    public void should_call_rest_client_with_correct_parameters() {
        // given
        String poBox = "samplePoBox";
        String url = "https://example.com/validate-ocr";
        String s2sToken = "sample-s2s-token";
        String subtype = "sample_document_subtype";
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            poBox,
            Classification.EXCEPTION,
            asList(
                doc(subtype, sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", poBox, url)
            ));

        given(client.validate(eq(url), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate())
            .willReturn(s2sToken);

        // when
        ocrValidator.assertIsValid(envelope);

        // then
        verify(client).validate(eq(url), argCaptor.capture(), eq(s2sToken));
        assertThat(argCaptor.getValue().type).isEqualTo(subtype);
        assertThat(argCaptor.getValue().ocrDataFields)
            .extracting(
                it -> tuple(it.name, it.value)
            )
            .containsExactlyElementsOf(sampleOcr().getFields().stream().map(
                it -> tuple(it.name.textValue(), it.value.textValue())).collect(toList())
            );
    }

    private InputOcrData sampleOcr() {
        InputOcrData data = new InputOcrData();
        data.setFields(asList(
            new InputOcrDataField(new TextNode("hello"), new TextNode("world")),
            new InputOcrDataField(new TextNode("foo"), new TextNode("bar"))
        ));
        return data;
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
