DROP MATERIALIZED VIEW metadata_language_summary_mv;
CREATE MATERIALIZED VIEW metadata_language_summary_mv AS
select
    i.uuid as item_id,
    i.last_modified,
    type_count,
    coalesce(type_en_count, 0) as type_en_count,
    (coalesce(subject_broad_count, 0) + coalesce(subject_descriptor_count, 0) +
     coalesce(subject_gc_count, 0) + coalesce(subject_geoscan_count, 0)) * 2 as subject_raw_count,
    coalesce(subject_en_count, 0) + coalesce(subject_fr_count, 0) as subject_curated_count
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
    (select dspace_object_id, count(distinct t.id) as subject_broad_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
              join term t on mdv.text_value = t.name_en or mdv.text_value = t.name_fr
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('broad')
       and t.vocabulary_id = 3
     group by dspace_object_id) subject_broad
    on typ.dspace_object_id = subject_broad.dspace_object_id
        left join
    (select dspace_object_id, count(distinct t.id) as subject_descriptor_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
              join term t on mdv.text_value = t.name_en or mdv.text_value = t.name_fr
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('descriptor')
       and t.vocabulary_id = 4
     group by dspace_object_id) subject_descriptor
    on typ.dspace_object_id = subject_descriptor.dspace_object_id
        left join
    (select dspace_object_id, count(distinct t.id) as subject_gc_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
              join term t on mdv.text_value = t.name_en or mdv.text_value = t.name_fr
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('gc')
       and t.vocabulary_id = 2
     group by dspace_object_id) subject_gc
    on typ.dspace_object_id = subject_gc.dspace_object_id
        left join
    (select dspace_object_id, count(distinct t.id) as subject_geoscan_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
              join term t on mdv.text_value = t.name_en or mdv.text_value = t.name_fr
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('geoscan')
       and t.vocabulary_id = 1
     group by dspace_object_id) subject_geoscan
    on typ.dspace_object_id = subject_geoscan.dspace_object_id
        left join
    (select dspace_object_id, count(*) as subject_en_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('broad_en','descriptor_en','gc_en','geoscan_en')
     group by dspace_object_id) subject_en
    on typ.dspace_object_id = subject_en.dspace_object_id
        left join
    (select dspace_object_id, count(*) as subject_fr_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('broad_fr','descriptor_fr','gc_en','geoscan_fr')
     group by dspace_object_id) subject_fr
    on typ.dspace_object_id = subject_fr.dspace_object_id
    where i.owning_collection is not null;

GRANT ALL ON metadata_language_summary_mv to dspace;

CREATE UNIQUE INDEX idx_mdls_item_id ON metadata_language_summary_mv (item_id);
