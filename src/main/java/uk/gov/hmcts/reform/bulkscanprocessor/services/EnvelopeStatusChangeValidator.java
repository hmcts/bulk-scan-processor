package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidStatusChangeException;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

@Service
public class EnvelopeStatusChangeValidator {

    private static final Map<Status, List<Status>> allowedTransitions =
        ImmutableMap.of(
            PROCESSED, singletonList(CONSUMED)
        );

    /**
     * Checks whether it's legal to transition from given status to another.
     */
    public void assertCanUpdate(Status from, Status to) {
        boolean ok =
            allowedTransitions
                .getOrDefault(from, emptyList())
                .contains(to);

        if (!ok) {
            throw new InvalidStatusChangeException("Cannot change from status " + from + " to status " + to);
        }
    }
}
