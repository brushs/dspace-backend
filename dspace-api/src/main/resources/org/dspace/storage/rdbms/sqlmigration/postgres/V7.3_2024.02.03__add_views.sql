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
    c.text_value as entity_type,
    b.text_value as first_name,
    e.text_value as last_name,
    f.text_value as dpsid,
    d.text_value as date_added
from
    (select dspace_object_id
     from metadatavalue
     group by dspace_object_id
    ) a
        join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 184) b
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
    on a.dspace_object_id = d.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 185) e
    on a.dspace_object_id = e.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 281) f
    on a.dspace_object_id = f.dspace_object_id;

CREATE OR REPLACE VIEW metadata_language_summary_v AS
select
    i.uuid as item_id,
    i.last_modified,
    type_count,
    coalesce(type_en_count, 0) as type_en_count,
    coalesce(subject_count, 0) as subject_count,
    coalesce(subject_en_count, 0) as subject_en_count
from
    item i
        join
    (select dspace_object_id, count(*) as type_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'type'
       and coalesce(mdv.text_lang, 'en') = 'en'
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
    (select dspace_object_id, count(*) as subject_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('cfs','broad','descriptor','gc','geoscan')
     group by dspace_object_id) subject
    on typ.dspace_object_id = subject.dspace_object_id
        left join
    (select dspace_object_id, count(*) as subject_en_count
     from metadatavalue mdv
              join metadatafieldregistry mfr on mdv.metadata_field_id = mfr.metadata_field_id
     where mfr.metadata_schema_id = 1
       and mfr.element = 'subject'
       and mfr.qualifier in ('cfs_en','broad_en','descriptor_en','gc_en','geoscan_en')
     group by dspace_object_id) subject_en
    on typ.dspace_object_id = subject_en.dspace_object_id;

