package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Consumes list of PDFs from an envelope (single zip file).
 */
@Component
public class PdfsConsumer implements Consumer<List<PDF>> {

    @Override
    public void accept(List<PDF> pdfs) {
        // To be implemented
    }
}
