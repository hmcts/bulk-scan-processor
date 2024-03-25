package uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache;

import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Expiry for IDAM cache.
 */
@Component
public class IdamCacheExpiry implements Expiry<String, CachedIdamCredential> {

    private final long refreshTokenBeforeExpiry;

    /**
     * Constructor for IdamCacheExpiry.
     * @param refreshTokenBeforeExpiry The refresh token before expiry
     */
    public IdamCacheExpiry(
        @Value("${idam.client.cache.refresh-before-expire-in-sec}") long refreshTokenBeforeExpiry
    ) {
        this.refreshTokenBeforeExpiry = refreshTokenBeforeExpiry;
    }

    /**
     * Gets the expiry after creation.
     * @param jurisdiction The jurisdiction
     * @param cachedIdamCredential The cached IDAM credential
     * @param currentTime The current time
     * @return The expiry after creation
     */
    @Override
    public long expireAfterCreate(
        @NonNull String jurisdiction,
        @NonNull CachedIdamCredential cachedIdamCredential,
        long currentTime
    ) {
        return TimeUnit.SECONDS.toNanos(cachedIdamCredential.expiresIn - refreshTokenBeforeExpiry);
    }

    /**
     * Gets the expiry after update.
     * @param jurisdiction The jurisdiction
     * @param cachedIdamCredential The cached IDAM credential
     * @param currentTime The current time
     * @param currentDuration The current duration
     * @return The expiry after update
     */
    @Override
    public long expireAfterUpdate(
        @NonNull String jurisdiction,
        @NonNull CachedIdamCredential cachedIdamCredential,
        long currentTime,
        @NonNegative long currentDuration
    ) {
        return TimeUnit.SECONDS.toNanos(cachedIdamCredential.expiresIn - refreshTokenBeforeExpiry);
    }

    /**
     * Gets the expiry after read.
     * @param jurisdiction The jurisdiction
     * @param cachedIdamCredential The cached IDAM credential
     * @param currentTime The current time
     * @param currentDuration The current duration
     * @return The expiry after read
     */
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
