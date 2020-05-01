CREATE INDEX scannable_items_envelope_id_idx ON scannable_items (envelope_id);
CREATE INDEX non_scannable_items_envelope_id_idx ON non_scannable_items (envelope_id);
CREATE INDEX payments_envelope_id_idx ON payments (envelope_id);
