package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "non_scannable_items")
public class NonScannableItem {

    @Id
    private UUID id;

    @JsonProperty("item_type")
    private String itemType;
    @JsonProperty("notes")
    private String notes;

    private NonScannableItem() {
        // For use by hibernate.
    }

    public NonScannableItem(
        @JsonProperty("item_type") String itemType,
        @JsonProperty("notes") String notes
    ) {
        this.itemType = itemType;
        this.notes = notes;
    }
}
