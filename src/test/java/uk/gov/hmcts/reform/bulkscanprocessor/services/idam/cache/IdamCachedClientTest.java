package uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.Credential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.JurisdictionToUserMapping;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class IdamCachedClientTest {

    @Mock
    private IdamClient idamApi;
    @Mock
    private JurisdictionToUserMapping users;

    private IdamCachedClient idamCachedClient;

    private long refreshTokenBeforeExpiry = 2879;

    private static final String JWT_1 = "JWT_1_assawdsa";
    private static final String JWT_2 = "JWT_2_56212398";

    private static final String JWT_WITH_BEARER_1 = "Bearer " + JWT_1;

    private static final String JWT_WITH_BEARER_2 = "Bearer " + JWT_2;

    private static final String USERNAME = "userxxx";

    private static final String PASSWORD = "passs123";

    private static final TokenResponse TOKEN_RESPONSE_1 = new TokenResponse(
        JWT_1,
        "28800",
        "ID_TOKEN_xxxx_123",
        "REFRESH_TOKEN_xyxyx123",
        "openid profile roles",
        "Bearer"
    );

    private static final TokenResponse TOKEN_RESPONSE_2 = new TokenResponse(
        JWT_2,
        "28800",
        "ID_TOKEN_xxxx_123",
        "REFRESH_TOKEN_xyxyx123",
        "openid profile roles",
        "Bearer"
    );

    private static final UserInfo USER_INFO = new UserInfo(
        "sub",
        "uid",
        "name",
        "givenname",
        "familyname",
        Arrays.asList("role1, role2", "role3")
    );

    @BeforeEach
    void setUp() {
        this.idamCachedClient = new IdamCachedClient(
            idamApi,
            users,
            new IdamCacheExpiry(refreshTokenBeforeExpiry)
        );
    }

    @Test
    void should_get_credentials_when_no_error() {
        String jurisdiction = "divorce";

        given(users.getUser(jurisdiction)).willReturn(new Credential(USERNAME, PASSWORD));
        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(USER_INFO);

        CachedIdamCredential cachedIdamCredential =
            idamCachedClient.getIdamCredentials(jurisdiction);

        assertThat(cachedIdamCredential.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential.userId).isEqualTo(USER_INFO.getUid());
        verify(users).getUser(jurisdiction);
        verify(idamApi).getAccessTokenResponse(USERNAME, PASSWORD);
        verify(idamApi).getUserInfo(JWT_WITH_BEARER_1);
    }

    @Test
    void should_cache_by_jurisdiction() {
        String jurisdiction1 = "divorce";
        String jurisdiction2 = "cmc";

        given(users.getUser(jurisdiction1)).willReturn(new Credential(USERNAME, PASSWORD));
        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1);
        UserInfo expectedUserDetails1 = USER_INFO;
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(expectedUserDetails1);

        UserInfo expectedUserDetails2 = new UserInfo("12", "123123", "q@a.com", "", "", null);
        given(users.getUser(jurisdiction2)).willReturn(new Credential(USERNAME + 2, PASSWORD + 2));
        given(idamApi.getAccessTokenResponse(USERNAME + 2, PASSWORD + 2)).willReturn(TOKEN_RESPONSE_2);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_2)).willReturn(expectedUserDetails2);

        CachedIdamCredential cachedIdamCredential1 =
            idamCachedClient.getIdamCredentials(jurisdiction1);
        CachedIdamCredential cachedIdamCredential2 =
            idamCachedClient.getIdamCredentials(jurisdiction2);

        assertThat(cachedIdamCredential1.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential1.userId).isEqualTo(expectedUserDetails1.getUid());
        assertThat(cachedIdamCredential2.accessToken).isEqualTo(JWT_WITH_BEARER_2);
        assertThat(cachedIdamCredential2.userId).isEqualTo(expectedUserDetails2.getUid());

        verify(users, times(2)).getUser(any());
        verify(idamApi, times(2)).getAccessTokenResponse(any(), any());
        verify(idamApi, times(2)).getUserInfo(any());

    }

    @Test
    void should_retrieve_token_from_cache_when_value_in_cache_case_insensitive() {
        String jurisdictionCaps = "BULKSCAN";
        String jurisdiction = "bulkscan";

        given(users.getUser(jurisdiction)).willReturn(new Credential(USERNAME, PASSWORD));
        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(USER_INFO);

        CachedIdamCredential cachedIdamCredential1 =
            idamCachedClient.getIdamCredentials(jurisdictionCaps);

        CachedIdamCredential cachedIdamCredential2 =
            idamCachedClient.getIdamCredentials(jurisdiction);

        assertThat(cachedIdamCredential1.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential1).usingRecursiveComparison().isEqualTo(
            cachedIdamCredential2);
        verify(users).getUser(any());
        verify(idamApi).getAccessTokenResponse(any(), any());
        verify(idamApi).getUserInfo(any());
    }

    @Test
    void should_create_token_when_cache_is_expired() throws InterruptedException {

        IdamCachedClient idamCachedClientQuickExpiry = new IdamCachedClient(
            idamApi,
            users,
            new IdamCacheExpiry(28798)
        );

        String jurisdiction = "probate";

        given(users.getUser(jurisdiction)).willReturn(new Credential(USERNAME, PASSWORD));
        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1, TOKEN_RESPONSE_2);

        UserInfo expectedUserDetails1 = USER_INFO;
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(expectedUserDetails1);
        UserInfo expectedUserDetails2 = new UserInfo("12", "a8da9s8", "q@a.com", "", "", null);
        given(idamApi.getUserInfo(JWT_WITH_BEARER_2)).willReturn(expectedUserDetails2);

        CachedIdamCredential cachedIdamCredential1 =
            idamCachedClientQuickExpiry.getIdamCredentials(jurisdiction);

        //2 seconds expiry, wait expiry
        TimeUnit.SECONDS.sleep(3);

        CachedIdamCredential cachedIdamCredential2 =
            idamCachedClientQuickExpiry.getIdamCredentials(jurisdiction);

        assertThat(cachedIdamCredential1.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential1.userId).isEqualTo(expectedUserDetails1.getUid());
        assertThat(cachedIdamCredential2.accessToken).isEqualTo(JWT_WITH_BEARER_2);
        assertThat(cachedIdamCredential2.userId).isEqualTo(expectedUserDetails2.getUid());

        assertThat(cachedIdamCredential1).isNotEqualTo(cachedIdamCredential2);
        verify(users, times(2)).getUser(any());
        verify(idamApi, times(2)).getAccessTokenResponse(any(), any());
        verify(idamApi, times(2)).getUserInfo(any());
    }

    @Test
    void should_create_new_token_when_token_removed_from_cache() {
        String jurisdiction = "probate";

        given(users.getUser(jurisdiction)).willReturn(new Credential(USERNAME, PASSWORD));
        given(idamApi.getAccessTokenResponse(USERNAME, PASSWORD)).willReturn(TOKEN_RESPONSE_1, TOKEN_RESPONSE_2);
        UserInfo expectedUserDetails1 = USER_INFO;
        given(idamApi.getUserInfo(JWT_WITH_BEARER_1)).willReturn(expectedUserDetails1);
        UserInfo expectedUserDetails2 = new UserInfo("1122", "add3rew", "12q@a.com", "joe", "doe", null);

        given(idamApi.getUserInfo(JWT_WITH_BEARER_2)).willReturn(expectedUserDetails2);

        CachedIdamCredential cachedIdamCredential1 = idamCachedClient.getIdamCredentials(jurisdiction);

        idamCachedClient.removeAccessTokenFromCache(jurisdiction);

        CachedIdamCredential cachedIdamCredential2 = idamCachedClient.getIdamCredentials(jurisdiction);

        assertThat(cachedIdamCredential1.accessToken).isEqualTo(JWT_WITH_BEARER_1);
        assertThat(cachedIdamCredential1.userId).isEqualTo(expectedUserDetails1.getUid());
        assertThat(cachedIdamCredential2.accessToken).isEqualTo(JWT_WITH_BEARER_2);
        assertThat(cachedIdamCredential2.userId).isEqualTo(expectedUserDetails2.getUid());

        verify(users, times(2)).getUser(any());
        verify(idamApi, times(2)).getAccessTokenResponse(any(), any());
        verify(idamApi, times(2)).getUserInfo(any());
    }
}
