package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeInfo;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toList;

@Service
public class IncompleteEnvelopesService {

    private final EnvelopeRepository envelopeRepository;
    private static final Logger log = LoggerFactory.getLogger(IncompleteEnvelopesService.class);

    public IncompleteEnvelopesService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

    public List<EnvelopeInfo> getIncompleteEnvelopes(int staleTimeHr) {
        return envelopeRepository
            .getIncompleteEnvelopesBefore(now().minus(staleTimeHr, HOURS))
            .stream()
            .map(envelope -> new EnvelopeInfo(
                     envelope.getContainer(),
                     envelope.getZipFileName(),
                     envelope.getId(),
                     envelope.getCreatedAt()
                 )
            )
            .collect(toList());
    }

    @Transactional
    public int deleteIncompleteEnvelopes(int staleTimeHr, List<String> envelopesToRemove) {
        List<UUID> envelopeIds = envelopesToRemove.stream()
            .map(UUID::fromString)
            .collect(Collectors.toList());

        if (!envelopeIds.isEmpty()) {
            int rowsDeleted = envelopeRepository.deleteEnvelopesBefore(
                now().minus(staleTimeHr, HOURS),
                envelopeIds
            );
            log.info("{} rows have been deleted: {}", rowsDeleted, envelopesToRemove);
            return rowsDeleted;
        } else {
            log.info("No stale envelopes older than a week found");
            return 0;
        }
    }
}
