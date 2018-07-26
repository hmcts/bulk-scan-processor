package uk.gov.hmcts.reform.bulkscanprocessor.services;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeAccessServiceTest {

    @Mock
    private EnvelopeAccessProperties accessProps;

    private EnvelopeAccessService service;

    @Before
    public void setUp() throws Exception {
        this.service = new EnvelopeAccessService(accessProps);
    }

    @Test
    public void assertCanUpdate_should_throw_an_exception_if_service_is_not_allowed_to_update_in_given_jurisdiction() {
        // given
        BDDMockito
            .given(accessProps.getMappings())
            .willReturn(asList(
                new Mapping("jur_A", "read_A", "update_A"),
                new Mapping("jur_B", "read_B", "update_B")
            ));

        // when
        Throwable error = catchThrowable(() -> service.assertCanUpdate("jur_A", "update_B"));

        // then
        assertThat(error)
            .isNotNull()
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("jur_A")
            .hasMessageContaining("update_B");
    }

    @Test
    public void assertCanUpdate_should_throw_an_exception_if_there_is_not_configuration_for_given_jurisdiction() {
        // given
        BDDMockito
            .given(accessProps.getMappings())
            .willReturn(singletonList(
                new Mapping("jur_A", "read_A", "update_A")
            ));

        // when
        Throwable error = catchThrowable(() -> service.assertCanUpdate("nonExistingJurisdiction", "update_A"));

        // then
        assertThat(error)
            .isNotNull()
            .isInstanceOf(ServiceConfigNotFoundException.class)
            .hasMessageContaining("nonExistingJurisdiction");
    }
}
