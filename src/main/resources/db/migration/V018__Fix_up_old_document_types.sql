UPDATE scannable_items SET documenttype = 'Cherished' WHERE documenttype IN ('CHERISHED', 'cherished');

UPDATE scannable_items SET documenttype = 'Other' WHERE documenttype != 'Cherished';
