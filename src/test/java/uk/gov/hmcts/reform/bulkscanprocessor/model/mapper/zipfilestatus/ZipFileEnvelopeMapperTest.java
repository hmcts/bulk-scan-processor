package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.zipfilestatus;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ZipFileEnvelopeMapperTest {

    @Test
    public void should_map_envelope_properly() {
        // given
        Envelope input = mock(Envelope.class);
        given(input.getId()).willReturn(UUID.randomUUID());
        given(input.getZipFileName()).willReturn("x.zip");
        given(input.getStatus()).willReturn(Status.PROCESSED);

        // when
        ZipFileEnvelope output = ZipFileEnvelopeMapper.fromEnvelope(input);

        // then
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(output.id).as("id").isEqualTo(input.getId().toString());
        softly.assertThat(output.container).as("container").isEqualTo(input.getContainer());
        softly.assertThat(output.status).as("status").isEqualTo(input.getStatus().name());

        softly.assertAll();
    }

    @Test
    public void should_handle_null() {
        // given
        Envelope input = null;

        // when
        ZipFileEnvelope output = ZipFileEnvelopeMapper.fromEnvelope(input);

        // then
        assertThat(output).isNull();
    }
}
