package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.BlobInfo;

import java.util.List;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toList;

@Service
public class IncompleteEnvelopesService {

    private final EnvelopeRepository envelopeRepository;

    public IncompleteEnvelopesService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

    public List<BlobInfo> getIncompleteEnvelopes(int staleTimeHr) {
        return envelopeRepository
            .getIncompleteEnvelopesBefore(now().minus(staleTimeHr, HOURS))
            .stream()
            .map(envelope -> new BlobInfo(
                     envelope.getContainer(),
                     envelope.getZipFileName(),
                     envelope.getCreatedAt()
                 )
            )
            .collect(toList());
    }
}
