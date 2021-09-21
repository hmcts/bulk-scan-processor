package uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IdamCacheExpiryTest {

    private IdamCacheExpiry idamCacheExpiry = new IdamCacheExpiry(20);

    private static final String USER_ID = "12";

    @ParameterizedTest
    @CsvSource({
        "20, 1212, 0",
        "2, 9812233, -18000000000",
        "22, 0, 2000000000"
    })
    void expireAfterCreate(long expireIn, long currentTime, long result) {
        CachedIdamCredential cachedIdamCredential = new CachedIdamCredential("token", USER_ID, expireIn);
        long remainingTime = idamCacheExpiry.expireAfterCreate(
            "key_9090",
            cachedIdamCredential,
            currentTime
        );
        assertThat(remainingTime).isEqualTo(result);
    }


    @ParameterizedTest
    @CsvSource({
        "20,  1212,    5,  0",
        "100,  123, 1200, 80",
        "22,   454,  420,  2"
    })
    void should_update_remaining_time_when_cached_value_is_updated(
        long expireIn,
        long currentTime,
        long currentSecondsLeft,
        long expectedSecondsLeft
    ) {
        // given
        CachedIdamCredential newCreds = new CachedIdamCredential("token", USER_ID, expireIn);

        // when
        long remainingTimeNanos = idamCacheExpiry.expireAfterUpdate(
            "someJurisdiction",
            newCreds,
            currentTime,
            TimeUnit.MILLISECONDS.toNanos(currentSecondsLeft)
        );

        // then
        assertThat(remainingTimeNanos).isEqualTo(TimeUnit.SECONDS.toNanos(expectedSecondsLeft));
    }

    @ParameterizedTest
    @CsvSource({
        "20, 122212, 0, 0",
        "2, 9812233, 1200, 1200",
        "22, 0, 120, 120"
    })
    void expireAfterRead(long expireIn, long currentTime, long currentDuration, long result) {
        CachedIdamCredential cachedIdamCredential = new CachedIdamCredential("token", USER_ID, expireIn);

        long remainingTime = idamCacheExpiry.expireAfterRead(
            "21321",
            cachedIdamCredential,
            currentTime,
            currentDuration
        );
        assertThat(remainingTime).isEqualTo(result);
    }
}
