package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.BlobInfo;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Service
public class IncompleteEnvelopesService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
                     toLocalTimeZone(envelope.getCreatedAt())
                 )
            )
            .collect(toList());
    }

    private static String toLocalTimeZone(Instant instant) {
        return dateTimeFormatter.format(ZonedDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID));
    }
}
