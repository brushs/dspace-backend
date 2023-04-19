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