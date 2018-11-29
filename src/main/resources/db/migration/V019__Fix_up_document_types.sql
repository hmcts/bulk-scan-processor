UPDATE scannable_items SET documenttype = 'CHERISHED' WHERE documenttype = 'Cherished';

UPDATE scannable_items SET documenttype = 'OTHER' WHERE documenttype != 'Cherished';
