
CREATE OR REPLACE VIEW translation_rels_v AS
SELECT DISTINCT
    mv.dspace_object_id AS source_dspace_object_id,
    CASE r1.right_id WHEN '676fccb3-0b45-41b0-ad49-3eaa3e7e866c' THEN 'en' ELSE 'fr' END AS source_lang,
    h.resource_id AS translated_dspace_object_id,
    CASE r2.right_id WHEN '676fccb3-0b45-41b0-ad49-3eaa3e7e866c' THEN 'en' ELSE 'fr' END AS translated_lang,
    (regexp_match(mv.text_value, '>([^<]+)</a>'))[1] AS translated_name
FROM metadatavalue mv
    CROSS JOIN LATERAL (
    SELECT (regexp_match(mv.text_value, '/handle/[^/]+/([^"<>/]+)'))[1] AS handle_suffix
    ) x
    JOIN handle h
ON regexp_replace(h.handle, '^.*/', '') = x.handle_suffix
    JOIN relationship r1
    ON r1.left_id = mv.dspace_object_id AND r1.type_id = 18
    JOIN relationship r2
    ON r2.left_id = h.resource_id AND r2.type_id = 18
WHERE mv.metadata_field_id = 282
  AND h.resource_type_id = 2
  AND x.handle_suffix IS NOT NULL;


