DROP MATERIALIZED VIEW metadata_language_summary_mv;
CREATE MATERIALIZED VIEW metadata_language_summary_mv AS
select
    i.uuid as item_id,
    i.last_modified,
    type_count,
    coalesce(type_en_count, 0) as type_en_count,
    (TO_TIMESTAMP(metadata_process_date, 'YYYY-MM-DD"T"HH24:MI:SS') + INTERVAL '1 minute') as metadata_process_date
from
    item i
        join
    (select dspace_object_id, count(*) as type_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'type'
       and (mdv.text_lang = 'en' or mdv.text_lang = '' or mdv.text_lang is null)
     group by dspace_object_id) typ
    on i.uuid = typ.dspace_object_id
        left join
    (select dspace_object_id, count(*) as type_en_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'type_en'
     group by dspace_object_id) type_en
    on typ.dspace_object_id = type_en.dspace_object_id
        left join
    (select dspace_object_id, text_value as metadata_process_date
         from metadatavalue mdv
                  join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
         where mfr.metadata_schema_id = 8
           and mfr.element = 'internal'
           and mfr.qualifier = 'metadataprocessdate') mpd
        on typ.dspace_object_id = mpd.dspace_object_id
    where i.owning_collection is not null;

GRANT ALL ON metadata_language_summary_mv to dspace;

CREATE UNIQUE INDEX idx_mdls_item_id ON metadata_language_summary_mv (item_id);
