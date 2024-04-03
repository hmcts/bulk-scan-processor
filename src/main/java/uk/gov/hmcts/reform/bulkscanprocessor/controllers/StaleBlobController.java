package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SearchResult;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.StaleBlobFinder;

/**
 * Controller for finding stale blobs.
 */
@RestController
@RequestMapping(path = "/stale-blobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class StaleBlobController {

    private final StaleBlobFinder staleBlobFinder;

    private static final String DEFAULT_STALE_TIME_MINUTES = "120";

    /**
     * Constructor for the stale blob controller.
     * @param staleBlobFinder The service for finding stale blobs
     */
    public StaleBlobController(StaleBlobFinder staleBlobFinder) {
        this.staleBlobFinder = staleBlobFinder;
    }

    /**
     * Find stale blobs.
     * @param staleTime The time after which a blob is considered stale
     * @return The search result
     */
    @GetMapping
    public SearchResult findStaleBlobs(
        @RequestParam(name = "stale_time", required = false, defaultValue = DEFAULT_STALE_TIME_MINUTES)
            int staleTime
    ) {
        return new SearchResult(staleBlobFinder.findStaleBlobs(staleTime));
    }
}
