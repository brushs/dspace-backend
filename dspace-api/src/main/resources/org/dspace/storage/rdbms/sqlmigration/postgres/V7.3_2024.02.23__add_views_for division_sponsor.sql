create or replace view divisions_v as
select
    mdvc.dspace_object_id,
    mdvc.code,
    mdvne.name_en,
    mdvnf.name_fr,
    mdvse.sector_en,
    mdvsf.sector_fr
from
    (select dspace_object_id, text_value as code
     from metadatavalue
     where metadata_field_id = 180) mdvc
        left outer join
    (select dspace_object_id, text_value as name_en
     from metadatavalue
     where metadata_field_id = 73
       and text_lang = 'en') mdvne
    on mdvne.dspace_object_id = mdvc.dspace_object_id
        left outer join
    (select dspace_object_id, text_value as name_fr
     from metadatavalue
     where metadata_field_id = 73
       and text_lang = 'fr') mdvnf
    on mdvnf.dspace_object_id = mdvc.dspace_object_id
        left outer join
    (select dspace_object_id, text_value as sector_en
     from metadatavalue
     where metadata_field_id = 182
       and text_lang = 'en') mdvse
    on mdvse.dspace_object_id = mdvc.dspace_object_id
        left outer join
    (select dspace_object_id, text_value as sector_fr
     from metadatavalue
     where metadata_field_id = 182
       and text_lang = 'fr') mdvsf
    on mdvsf.dspace_object_id = mdvc.dspace_object_id;

create or replace view sponsors_v as
select
    mdvc.dspace_object_id,
    mdvc.code,
    mdvpje.project_en,
    mdvpjf.project_fr,
    mdvpge.program_en,
    mdvpgf.program_fr
from
    (select dspace_object_id, text_value as code
     from metadatavalue
     where metadata_field_id = 187) mdvc
        left outer join
    (select dspace_object_id, text_value as project_en
     from metadatavalue
     where metadata_field_id = 73
       and text_lang = 'en') mdvpje
    on mdvpje.dspace_object_id = mdvc.dspace_object_id
        left outer join
    (select dspace_object_id, text_value as project_fr
     from metadatavalue
     where metadata_field_id = 73
       and text_lang = 'fr') mdvpjf
    on mdvpjf.dspace_object_id = mdvc.dspace_object_id
        left outer join
    (select dspace_object_id, text_value as program_en
     from metadatavalue
     where metadata_field_id = 189
       and text_lang = 'en') mdvpge
    on mdvpge.dspace_object_id = mdvc.dspace_object_id
        left outer join
    (select dspace_object_id, text_value as program_fr
     from metadatavalue
     where metadata_field_id = 189
       and text_lang = 'fr') mdvpgf
    on mdvpgf.dspace_object_id = mdvc.dspace_object_id
order by code;
