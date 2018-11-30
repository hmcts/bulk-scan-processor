//package uk.gov.hmcts.reform.bulkscanprocessor.entity;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.util.UUID;
//import javax.persistence.Entity;
//import javax.persistence.FetchType;
//import javax.persistence.GeneratedValue;
//import javax.persistence.Id;
//import javax.persistence.JoinColumn;
//import javax.persistence.ManyToOne;
//import javax.persistence.Table;
//
//@Entity
//@Table(name = "non_scannable_items")
//public class NonScannableItem implements EnvelopeAssignable {
//
//    @Id
//    @GeneratedValue
//    private UUID id;
//
//    private String documentControlNumber;
//
//    private String itemType;
//
//    private String notes;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "envelope_id", nullable = false)
//    private Envelope envelope;
//
//    private NonScannableItem() {
//        // For use by hibernate.
//    }
//
//    public NonScannableItem(
//        @JsonProperty("document_control_number") String documentControlNumber,
//        @JsonProperty("item_type") String itemType,
//        @JsonProperty("notes") String notes
//    ) {
//        this.documentControlNumber = documentControlNumber;
//        this.itemType = itemType;
//        this.notes = notes;
//    }
//
//    public String getDocumentControlNumber() {
//        return documentControlNumber;
//    }
//
//    public String getItemType() {
//        return itemType;
//    }
//
//    public String getNotes() {
//        return notes;
//    }
//
//    @Override
//    public void setEnvelope(Envelope envelope) {
//        this.envelope = envelope;
//    }
//}
