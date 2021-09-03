package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.ToStringComparator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EnvelopeResponseMapperTest {

    private Envelope envelope;

    @BeforeEach
    void setUp() {
        envelope = EnvelopeCreator.envelope();
    }

    @Test
    void should_map_envelope_to_envelope_response() {
        EnvelopeResponse response = EnvelopeResponseMapper.toEnvelopeResponse(envelope);

        assertThat(response)
            .usingRecursiveComparison(
                RecursiveComparisonConfiguration
                    .builder()
                    .withComparatorForFields(
                        new ToStringComparator<Classification>(),
                        new String[]{"classification"}
                    ).build()
            )
            .ignoringFields("scannableItems.hasOcrData").isEqualTo(envelope);
    }
}
