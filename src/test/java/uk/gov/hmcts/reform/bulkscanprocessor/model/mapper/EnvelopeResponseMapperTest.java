package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.DbEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.ToStringComparator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvelopeResponseMapperTest {

    private DbEnvelope envelope;

    @Before
    public void setUp() {
        envelope = EnvelopeCreator.envelope();
    }

    @Test
    public void should_map_envelope_to_envelope_response() {
        EnvelopeResponse response = EnvelopeResponseMapper.toEnvelopeResponse(envelope);

        assertThat(response)
            .usingComparatorForFields(
                new ToStringComparator<Classification>(), new String[]{"classification"}
            )
            .isEqualToComparingFieldByFieldRecursively(envelope);
    }

}
