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
    coalesce(mono_author_count,0) as mono_author_count,
	n.text_value as email,
	o.text_value as external_author
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
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 357) n
    on a.dspace_object_id = n.dspace_object_id
        left outer join
    (select text_value, dspace_object_id
     from metadatavalue
     where metadata_field_id = 220) o
    on a.dspace_object_id = o.dspace_object_id
        left outer join
    (select count(*) as author_count, right_id
     from relationship
     where type_id = 1
     group by right_id) j
    on a.dspace_object_id = j.right_id
        left outer join
    (select count(*) as mono_author_count, right_id
     from relationship
     where type_id = 20
     group by right_id) k
    on a.dspace_object_id = k.right_id;
