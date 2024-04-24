CREATE OR REPLACE VIEW item_summary_v AS
select
    a.dspace_object_id,
    c.text_value as entity_type,
    b.text_value as title,
    d.text_value as date_added
from
    (select dspace_object_id
     from metadatavalue
     group by dspace_object_id
    ) a
        join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 73 and text_lang = 'en') b
    on a.dspace_object_id = b.dspace_object_id
        join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 7) c
    on a.dspace_object_id = c.dspace_object_id
        join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 18) d
    on a.dspace_object_id = d.dspace_object_id;

CREATE OR REPLACE VIEW person_summary_v AS
select
    a.dspace_object_id,
    b.text_value as first_name,
    e.text_value as last_name,
    m.text_value as orcid,
    g.text_value as migration_id,
    f.text_value as dpsid,
    h.text_value as nrnuserid,
    i.text_value as cfsid,
    d.text_value as date_added,
    coalesce(author_count,0) as author_count,
    coalesce(mono_author_count,0) as mono_author_count
from
    (select dspace_object_id
     from metadatavalue
     group by dspace_object_id
    ) a
        join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 353) b
    on a.dspace_object_id = b.dspace_object_id
        join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 18) d
    on a.dspace_object_id = d.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 354) e
    on a.dspace_object_id = e.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 465) f
    on a.dspace_object_id = f.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 217) g
    on a.dspace_object_id = g.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 466) h
    on a.dspace_object_id = h.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 457) i
    on a.dspace_object_id = i.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 365) m
    on a.dspace_object_id = m.dspace_object_id
        left outer join
    (select count(*) as author_count, right_id
     from relationship
     where type_id = 1
     group by right_id) j
    on a.dspace_object_id = j.right_id
        left outer join
    (select count(*) as mono_author_count, right_id
     from relationship
     where type_id = 2
     group by right_id) k
    on a.dspace_object_id = k.right_id;

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
    on typ.dspace_object_id = subject_fr.dspace_object_id;

