package uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache;

import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IdamCacheExpiry implements Expiry<String, CachedIdamCredential> {

    private final long refreshTokenBeforeExpiry;

    public IdamCacheExpiry(
        @Value("${idam.client.cache.refresh-before-expire-in-sec}") long refreshTokenBeforeExpiry
    ) {
        this.refreshTokenBeforeExpiry = refreshTokenBeforeExpiry;
    }

    @Override
    public long expireAfterCreate(
        @NonNull String jurisdiction,
        @NonNull CachedIdamCredential cachedIdamCredential,
        long currentTime
    ) {
        return TimeUnit.SECONDS.toNanos(cachedIdamCredential.expiresIn - refreshTokenBeforeExpiry);
    }

    @Override
    public long expireAfterUpdate(
        @NonNull String jurisdiction,
        @NonNull CachedIdamCredential cachedIdamCredential,
        long currentTime,
        @NonNegative long currentDuration
    ) {
        return TimeUnit.SECONDS.toNanos(cachedIdamCredential.expiresIn - refreshTokenBeforeExpiry);
    }

    @Override
    public long expireAfterRead(
        @NonNull String jurisdiction,
        @NonNull CachedIdamCredential cachedIdamCredential,
        long currentTime,
        @NonNegative long currentDuration
    ) {
        return currentDuration;
    }
}
