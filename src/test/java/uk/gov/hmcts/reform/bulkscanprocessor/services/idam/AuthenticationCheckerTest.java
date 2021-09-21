package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

import com.google.common.collect.ImmutableMap;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.IdamCachedClient;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthenticationCheckerTest {

    public static final String USER_TOKEN = "USER_token";
    public static final String USER_ID = "USER_ID";

    private static final String SUCCESSFUL_JURISDICTION = "jurisdiction";
    private static final String SUCCESSFUL_JURISDICTION_USERNAME = "username1";
    private static final String SUCCESSFUL_JURISDICTION_PASSWORD = "password1";

    private static final String LOCKED_ACCOUNT_JURISDICTION = "locked";
    private static final String LOCKED_ACCOUNT_JURISDICTION_USERNAME = "username2";
    private static final String LOCKED_ACCOUNT_JURISDICTION_PASSWORD = "password2";

    private static final Map<String, Map<String, String>> USERS = ImmutableMap.of(
        SUCCESSFUL_JURISDICTION, ImmutableMap.of(
            "username", SUCCESSFUL_JURISDICTION_USERNAME,
            "password", SUCCESSFUL_JURISDICTION_PASSWORD
        ),
        LOCKED_ACCOUNT_JURISDICTION, ImmutableMap.of(
            "username", LOCKED_ACCOUNT_JURISDICTION_USERNAME,
            "password", LOCKED_ACCOUNT_JURISDICTION_PASSWORD
        )
    );

    private static final CachedIdamCredential CACHED_IDAM_CREDENTIAL = new CachedIdamCredential(
        USER_TOKEN,
        USER_ID,
        28800L
    );

    @Mock
    private IdamCachedClient idamClient;

    private AuthenticationChecker authenticationChecker;

    @BeforeEach
    void setUp() {
        JurisdictionToUserMapping mapping = new JurisdictionToUserMapping();
        mapping.setUsers(USERS);

        authenticationChecker = new AuthenticationChecker(mapping, idamClient);
    }

    @Test
    void checkSignInForJurisdiction_should_return_success_for_successfully_authenticated_jurisdiction() {
        willReturn(CACHED_IDAM_CREDENTIAL)
            .given(idamClient)
            .getIdamCredentials(
                SUCCESSFUL_JURISDICTION
            );


        JurisdictionConfigurationStatus status =
            authenticationChecker.checkSignInForJurisdiction(SUCCESSFUL_JURISDICTION);

        assertThat(status.jurisdiction).isEqualTo(SUCCESSFUL_JURISDICTION);
        assertThat(status.isCorrect).isTrue();
        assertThat(status.errorDescription).isNull();
        assertThat(status.errorResponseStatus).isNull();
    }

    @Test
    void checkSignInForJurisdiction_should_return_failure_for_unsuccessfully_authenticated_jurisdiction() {
        FeignException exception = createFeignException(HttpStatus.LOCKED.value());

        willThrow(exception)
            .given(idamClient)
            .getIdamCredentials(LOCKED_ACCOUNT_JURISDICTION);

        JurisdictionConfigurationStatus status =
            authenticationChecker.checkSignInForJurisdiction(LOCKED_ACCOUNT_JURISDICTION);

        assertThat(status.jurisdiction).isEqualTo(LOCKED_ACCOUNT_JURISDICTION);
        assertThat(status.isCorrect).isFalse();
        assertThat(status.errorResponseStatus).isEqualTo(HttpStatus.LOCKED.value());
        assertThat(status.errorDescription).isEqualTo(exception.contentUTF8());
    }

    @Test
    void checkSignInForJurisdiction_should_return_failure_for_jurisdiction_missing_in_config() {
        String unknownJurisdiction = "unknown";

        assertThat(
            authenticationChecker.checkSignInForJurisdiction(unknownJurisdiction)
        )
            .isEqualToComparingFieldByField(
                new JurisdictionConfigurationStatus(
                    unknownJurisdiction,
                    false,
                    String.format("No user configured for jurisdiction: %s", unknownJurisdiction),
                    null
                )
            );

        verify(idamClient, never()).getIdamCredentials(anyString());
    }

    @Test
    void checkSignInForJurisdiction_should_return_failure_when_idam_call_fails() {
        String errorMessage = "test exception";
        RuntimeException exception = new RuntimeException(errorMessage);

        willThrow(exception).given(idamClient).getIdamCredentials(any());

        assertThat(authenticationChecker.checkSignInForJurisdiction(LOCKED_ACCOUNT_JURISDICTION))
            .isEqualToComparingFieldByField(new JurisdictionConfigurationStatus(
                LOCKED_ACCOUNT_JURISDICTION,
                false,
                errorMessage,
                null
            ));
    }

    @Test
    void checkSignInForAllJurisdictions_should_return_statuses_of_all_jurisdictions() {
        willReturn(CACHED_IDAM_CREDENTIAL)
            .given(idamClient)
            .getIdamCredentials(SUCCESSFUL_JURISDICTION);

        willThrow(createFeignException(HttpStatus.LOCKED.value()))
            .given(idamClient)
            .getIdamCredentials(LOCKED_ACCOUNT_JURISDICTION);

        assertThat(authenticationChecker.checkSignInForAllJurisdictions())
            .extracting(status -> tuple(status.jurisdiction, status.isCorrect, status.errorResponseStatus))
            .containsExactlyInAnyOrder(
                tuple(SUCCESSFUL_JURISDICTION, true, null),
                tuple(LOCKED_ACCOUNT_JURISDICTION, false, HttpStatus.LOCKED.value())
            )
            .as("Result should contain a correct entry for each configured jurisdiction");
    }

    private FeignException createFeignException(int httpStatus) {
        return FeignException
            .errorStatus("method1", Response
                .builder()
                .request(mock(Request.class))
                .body("Error response from IDAM".getBytes())
                .headers(Collections.emptyMap())
                .status(httpStatus)
                .build()
            );
    }
}
