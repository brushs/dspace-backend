INSERT INTO metadatavalue (
    SELECT NEXTVAL('metadatavalue_seq'), 417, mdv.title, null, 0, null, -1, mdv.dspace_object_id
    FROM item_summary_v mdv
    WHERE entity_type = 'Publisher'
);
