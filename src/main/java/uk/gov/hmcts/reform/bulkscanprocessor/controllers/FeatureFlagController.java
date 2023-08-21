package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.launchdarkly.LaunchDarklyClient;

import static org.springframework.http.ResponseEntity.ok;

@RateLimiter(name = "default")
@RestController
public class FeatureFlagController {
    private final LaunchDarklyClient featureToggleService;

    public FeatureFlagController(LaunchDarklyClient featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    @GetMapping("/feature-flags/{flag}")
    public ResponseEntity<String> flagStatus(@PathVariable String flag) {
        boolean isEnabled = featureToggleService.isFeatureEnabled(flag);
        return ok(flag + " : " + isEnabled);
    }
}
