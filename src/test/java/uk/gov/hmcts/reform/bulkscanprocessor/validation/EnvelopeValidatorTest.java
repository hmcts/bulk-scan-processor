package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvelopeValidatorTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String PO_BOX_1 = "sample PO box 1";
    private static final String PO_BOX_2 = "sample PO box 2";
    private static final String JURISDICTION = "jurisdiction";
    private static final String CONTAINER = "container";

    private EnvelopeValidator envelopeValidator;

    @BeforeEach
    void setUp() {
        envelopeValidator = new EnvelopeValidator();
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_passes_if_jurisdiction_and_pobox_are_correct() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_1.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                        singletonList(m),
                        envelope,
                        CONTAINER
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_passes_if_jurisdiction_and_pobox_are_correct_multiple_poboxes() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                asList(PO_BOX_1, PO_BOX_2),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                        singletonList(m),
                        envelope,
                        CONTAINER
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_throws_if_pobox_is_incorrect() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThrows(
            ContainerJurisdictionPoBoxMismatchException.class,
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                    singletonList(m),
                    envelope,
                    CONTAINER
            )
        );
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_throws_if_jurisdiction_is_incorrect() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope("wrong", PO_BOX_1.toUpperCase());

        // when
        // then
        assertThrows(
            ContainerJurisdictionPoBoxMismatchException.class,
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                    singletonList(m),
                    envelope,
                    CONTAINER
            )
        );
    }

    @Test
    void assertServiceEnabled_passes_if_pobox_is_correct() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_1.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertServiceEnabled(
                        envelope,
                        singletonList(m)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertServiceEnabled_passes_if_pobox_is_correct_multiple_poboxes() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                asList(PO_BOX_1, PO_BOX_2),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertServiceEnabled(
                        envelope,
                        singletonList(m)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertServiceEnabled_throws_if_pobox_is_incorrect() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThrows(
            ServiceDisabledException.class,
            () -> envelopeValidator.assertServiceEnabled(
                    envelope,
                    singletonList(m)
            )
        );
    }
}
