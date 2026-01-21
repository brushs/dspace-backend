-- DSpace Reporting Schema (UUID-first, monthly facts)
-- Generated: 2025-11-12
-- Notes:
-- - Dimensions use current snapshot only (Type 1 overwrite).
-- - item_uuid and bitstream_uuid are the primary keys used across facts/bridges.
-- - Facts are monthly-grain using ym_key (YYYYMM).

-- =========================
-- 0) SCHEMA (optional)
-- =========================
--DROP SCHEMA stats CASCADE;
CREATE SCHEMA IF NOT EXISTS stats;
SET search_path = stats, public;

-- =========================
-- 1) DIMENSIONS
-- =========================

-- Month dimension
CREATE TABLE IF NOT EXISTS stats.dim_month (
  ym_key        int PRIMARY KEY,        -- e.g., 202501
  year          int    NOT NULL,
  month         int    NOT NULL,        -- 1..12
  month_name    text   NOT NULL,
  first_day     date   NOT NULL,
  last_day      date   NOT NULL
);

-- Item (Publication) - current snapshot only
CREATE TABLE IF NOT EXISTS stats.dim_item (
  item_uuid          uuid PRIMARY KEY,
  handle             text,
  doi                text,
  title_en           text,
  title_fr           text,
  document_type      text,
  publication_year   int,
  owning_collection  text,
  withdrawn			 boolean
);

-- Authors (optional, many-to-many)
CREATE TABLE IF NOT EXISTS stats.dim_author (
  author_id   bigserial PRIMARY KEY,
  author_name text NOT NULL,
  orcid       text
);

CREATE TABLE IF NOT EXISTS stats.br_item_author (
  item_uuid   uuid   NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  author_id   bigint NOT NULL REFERENCES stats.dim_author(author_id) ON DELETE CASCADE,
  author_order int,
  PRIMARY KEY (item_uuid, author_id)
);

-- Subjects (optional, many-to-many)
CREATE TABLE IF NOT EXISTS stats.dim_subject (
  subject_id   bigserial PRIMARY KEY,
  subject_term text NOT NULL
);

CREATE TABLE IF NOT EXISTS stats.br_item_subject (
  item_uuid  uuid   NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  subject_id bigint NOT NULL REFERENCES stats.dim_subject(subject_id) ON DELETE CASCADE,
  PRIMARY KEY (item_uuid, subject_id)
);

-- Bitstreams (for mapping downloads -> items)
CREATE TABLE IF NOT EXISTS stats.dim_bitstream (
  bitstream_uuid  uuid PRIMARY KEY,
  item_uuid       uuid NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  file_name       text,
  mime_type       text,
  bundle_name     text
);

CREATE TABLE IF NOT EXISTS stats.dim_externalurl (
  cleaned_filename  text PRIMARY KEY,
  item_uuid       uuid NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  full_url       text
);

CREATE TABLE IF NOT EXISTS stats.stg_dim_externalurl (
  full_url       text PRIMARY KEY,
  item_uuid       uuid NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  cleaned_filename  text
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS ix_dim_item_handle          ON stats.dim_item (handle);
CREATE INDEX IF NOT EXISTS ix_dim_bitstream_item_uuid  ON stats.dim_bitstream (item_uuid);
CREATE INDEX IF NOT EXISTS ix_br_item_author_author_id ON stats.br_item_author (author_id);
CREATE INDEX IF NOT EXISTS ix_br_item_subject_subject  ON stats.br_item_subject (subject_id);

CREATE OR REPLACE VIEW stats.v_stg_dim_externalurl AS
SELECT cleaned_filename, max(item_uuid::text) as item_uuid, max(full_url) as full_url
FROM stats.stg_dim_externalurl
WHERE trim(cleaned_filename) <> ''
AND trim(cleaned_filename) <> '/'
GROUP BY cleaned_filename;

-- =========================
-- 2) FACTS (Monthly)
-- =========================

-- Pageviews by item/month (GA)
CREATE TABLE IF NOT EXISTS stats.fact_item_pageviews_monthly (
  item_uuid  uuid NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  ym_key     int  NOT NULL REFERENCES stats.dim_month(ym_key),
  pageviews  bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (item_uuid, ym_key)
);

-- Downloads by bitstream/month (source grain)
CREATE TABLE IF NOT EXISTS stats.fact_bitstream_downloads_monthly (
  bitstream_uuid uuid NOT NULL REFERENCES stats.dim_bitstream(bitstream_uuid) ON DELETE CASCADE,
  ym_key         int  NOT NULL REFERENCES stats.dim_month(ym_key),
  downloads      bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (bitstream_uuid, ym_key)
);

CREATE TABLE IF NOT EXISTS stats.fact_externalurl_downloads_monthly (
  cleaned_filename text NOT NULL REFERENCES stats.dim_externalurl(cleaned_filename) ON DELETE CASCADE,
  ym_key         int  NOT NULL REFERENCES stats.dim_month(ym_key),
  downloads      bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (cleaned_filename, ym_key)
);

-- Covering indexes
CREATE INDEX IF NOT EXISTS ix_fact_pv_ym_item  ON stats.fact_item_pageviews_monthly (ym_key, item_uuid);
CREATE INDEX IF NOT EXISTS ix_fact_dl_ym_bs    ON stats.fact_bitstream_downloads_monthly (ym_key, bitstream_uuid);

-- =========================
-- 3) STAGING (for loads)
-- =========================

-- From GA exports: Item UUID + month + pageviews
CREATE TABLE IF NOT EXISTS stats.stg_ga_item_monthly (
  item_uuid uuid NOT NULL,
  ym_key    int  NOT NULL,
  pageviews bigint NOT NULL
);

-- From logs/exports: Bitstream UUID + month + downloads
CREATE TABLE IF NOT EXISTS stats.stg_bitstream_downloads_monthly (
  bitstream_uuid uuid NOT NULL,
  ym_key         int  NOT NULL,
  downloads      bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS stats.stg_externalurl_downloads_monthly (
  cleaned_filename text NOT NULL,
  ym_key         int  NOT NULL,
  downloads      bigint NOT NULL
);

-- =========================
-- 4) MATERIALIZED VIEWS (flatten DSpace to current snapshot)
--    Replace SELECTs with your real joins into item/bitstream tables.
-- =========================

-- Drop+create pattern for MVs (IF EXISTS supported on DROP)
DROP MATERIALIZED VIEW IF EXISTS stats.mv_item_current;
DROP MATERIALIZED VIEW IF EXISTS stats.mv_item_current_raw;

CREATE MATERIALIZED VIEW stats.mv_item_current_raw AS
SELECT
  i.uuid                       AS item_uuid,
  h.handle                     AS handle,
  doi_md.text_value                 AS doi,
  title_en_md.text_value            AS title_en,
  title_fr_md.text_value            AS title_fr,
  type_md.text_value                AS document_type,
  SUBSTRING(pubyear_md.text_value, 0, 4)::int        AS publication_year,
  col_md.text_value                 AS owning_collection,
  i.withdrawn						AS withdrawn
FROM item i
JOIN metadatavalue entity_type_md    ON entity_type_md.dspace_object_id = i.uuid AND entity_type_md.metadata_field_id = 7 and text_value = 'Publication'
LEFT JOIN handle h                   ON h.resource_id = i.uuid AND h.resource_type_id = 2
LEFT JOIN metadatavalue doi_md     ON doi_md.dspace_object_id = i.uuid AND doi_md.metadata_field_id = 32
LEFT JOIN metadatavalue title_en_md   ON title_en_md.dspace_object_id = i.uuid AND title_en_md.metadata_field_id = 73 AND title_en_md.text_lang = 'en'
LEFT JOIN metadatavalue title_fr_md   ON title_fr_md.dspace_object_id = i.uuid AND title_fr_md.metadata_field_id = 73 AND title_fr_md.text_lang = 'fr'
LEFT JOIN metadatavalue type_md    ON type_md.dspace_object_id = i.uuid AND type_md.metadata_field_id = 490
LEFT JOIN metadatavalue pubyear_md ON pubyear_md.dspace_object_id = i.uuid AND pubyear_md.metadata_field_id = 22
LEFT JOIN collection coll           ON coll.uuid = i.owning_collection
LEFT JOIN metadatavalue col_md		ON coll.uuid = col_md.dspace_object_id AND col_md.metadata_field_id = 73 AND col_md.text_lang = 'en';

CREATE MATERIALIZED VIEW stats.mv_item_current AS
SELECT
  item_uuid,
  MAX(handle)                                          AS handle,
  MAX(doi)											   AS doi,
  MAX(title_en)                                        AS title_en,
  MAX(title_fr)                                        AS title_fr,
  MAX(document_type)                                   AS document_type,
  MAX(publication_year)             							   AS publication_year,
  MAX(owning_collection)                               AS owning_collection,
  withdrawn											AS withdrawn
FROM stats.mv_item_current_raw
GROUP BY item_uuid, withdrawn;

-- Optional: index on MV
CREATE INDEX IF NOT EXISTS ix_mv_item_current_uuid ON stats.mv_item_current (item_uuid);

DROP MATERIALIZED VIEW IF EXISTS stats.mv_bitstreams;
CREATE MATERIALIZED VIEW stats.mv_bitstreams AS
SELECT
  b.uuid          AS bitstream_uuid,
  i.uuid          AS item_uuid,
  mdv.text_value  AS file_name
FROM bitstream b
JOIN bundle2bitstream bb ON bb.bitstream_id = b.uuid
JOIN bundle bun          ON bun.uuid = bb.bundle_id
JOIN item2bundle ib      ON ib.bundle_id = bun.uuid
JOIN item i              ON i.uuid = ib.item_id
JOIN metadatavalue entity_type_md    ON entity_type_md.dspace_object_id = i.uuid AND entity_type_md.metadata_field_id = 7 and text_value = 'Publication'
LEFT JOIN metadatavalue mdv   ON b.uuid = mdv.dspace_object_id AND mdv.metadata_field_id = 73;

CREATE INDEX IF NOT EXISTS ix_mv_bs_uuid ON stats.mv_bitstreams (bitstream_uuid);
CREATE INDEX IF NOT EXISTS ix_mv_bs_item ON stats.mv_bitstreams (item_uuid);

DROP MATERIALIZED VIEW IF EXISTS stats.mv_item_externalurl;

CREATE MATERIALIZED VIEW stats.mv_item_externalurl AS
SELECT
	mdv.dspace_object_id        AS item_uuid,
	text_value 					AS full_url
FROM metadatavalue mdv
WHERE metadata_field_id in (222,496);

-- =========================
-- 6) REPORTING VIEWS
-- =========================

-- Item-level downloads (aggregated from bitstream grain)
CREATE OR REPLACE VIEW stats.v_item_bitstream_downloads_monthly AS
SELECT
  b.item_uuid,
  f.ym_key,
  SUM(f.downloads) AS downloads
FROM stats.fact_bitstream_downloads_monthly f
JOIN stats.dim_bitstream b ON b.bitstream_uuid = f.bitstream_uuid
GROUP BY b.item_uuid, f.ym_key;

CREATE OR REPLACE VIEW stats.v_item_externalurl_downloads_monthly AS
SELECT
  e.item_uuid,
  f.ym_key,
  SUM(f.downloads) AS downloads
FROM stats.fact_externalurl_downloads_monthly f
JOIN stats.dim_externalurl e ON e.cleaned_filename = f.cleaned_filename
GROUP BY e.item_uuid, f.ym_key;

CREATE OR REPLACE VIEW stats.v_total_downloads_monthly AS
SELECT
    item_uuid,
    ym_key,
    SUM(downloads) AS downloads
FROM (
    SELECT
        item_uuid,
        ym_key,
        downloads
    FROM stats.v_item_bitstream_downloads_monthly
    UNION ALL
    SELECT
        item_uuid,
        ym_key,
        downloads
    FROM stats.v_item_externalurl_downloads_monthly
) AS combined
GROUP BY
    item_uuid,
    ym_key;

-- Unified usage view (pageviews + downloads)
CREATE OR REPLACE VIEW stats.v_item_usage_monthly AS
SELECT
  i.item_uuid,
  i.title_en,
  i.title_fr,
  i.document_type,
  i.owning_collection,
  m.ym_key,
  m.year,
  m.month,
  COALESCE(pv.pageviews, 0) AS pageviews,
  COALESCE(dw.downloads, 0) AS downloads
FROM stats.dim_item i
CROSS JOIN stats.dim_month m
LEFT JOIN stats.fact_item_pageviews_monthly pv
       ON pv.item_uuid = i.item_uuid AND pv.ym_key = m.ym_key
LEFT JOIN stats.v_total_downloads_monthly dw
       ON dw.item_uuid = i.item_uuid AND dw.ym_key = m.ym_key;

CREATE OR REPLACE VIEW stats.v_item_language AS
SELECT
    di.item_uuid,
    (ren.id IS NOT NULL) as en,
    (rfr.id IS NOT NULL) as fr,
    (mdv1.metadata_value_id IS NOT NULL) AS is_translation,
    (trans.handle IS NOT NULL) as has_translation,
    ((NOT (ren.id IS NOT NULL)
                OR
      NOT (rfr.id IS NOT NULL))
     AND
     (NOT (mdv1.metadata_value_id IS NOT NULL)
                AND
      NOT (trans.handle IS NOT NULL))) AS unilingual
FROM stats.dim_item di
LEFT JOIN relationship rfr ON rfr.type_id = 18 AND rfr.left_id = di.item_uuid
            AND rfr.right_id = 'ddf014c4-9d2f-4497-bbd1-1417bf9d5468'
LEFT JOIN relationship ren ON ren.type_id = 18 AND ren.left_id = di.item_uuid
            AND ren.right_id = '676fccb3-0b45-41b0-ad49-3eaa3e7e866c'
LEFT JOIN metadatavalue mdv1 ON mdv1.dspace_object_id = di.item_uuid AND mdv1.metadata_field_id = 282
LEFT JOIN (SELECT substring(mdv2.text_value FROM 'href="([^"]*)"') AS handle
            FROM metadatavalue mdv2
            WHERE mdv2.metadata_field_id = 282) trans ON trans.handle = di.handle;

CREATE OR REPLACE VIEW stats.v_item_language AS
select
            di.item_uuid,
            (ren.id IS NOT NULL) as en,
            (rfr.id IS NOT NULL) as fr,
            (mdv1.metadata_value_id IS NOT NULL) as is_translation,
            (trans.handle IS NOT NULL) as has_translation,
            ((NOT (ren.id IS NOT NULL)
                        OR
              NOT (rfr.id IS NOT NULL))
             AND
             (NOT (mdv1.metadata_value_id IS NOT NULL)
                        AND
              NOT (trans.handle IS NOT NULL))) AS unilingual
from stats.dim_item di
left join relationship rfr on rfr.type_id = 18 and rfr.left_id = di.item_uuid
            and rfr.right_id = 'ddf014c4-9d2f-4497-bbd1-1417bf9d5468'
left join relationship ren on ren.type_id = 18 and ren.left_id = di.item_uuid
            and ren.right_id = '676fccb3-0b45-41b0-ad49-3eaa3e7e866c'
left join metadatavalue mdv1 on mdv1.dspace_object_id = di.item_uuid and mdv1.metadata_field_id = 282
left join (select substring(mdv2.text_value FROM 'href="([^"]*)"') as handle
                                    from metadatavalue mdv2
                                    where mdv2.metadata_field_id = 282) trans ON trans.handle = di.handle;

CREATE OR REPLACE VIEW stats.v_report_downloads_language AS
select
            di.title_en,
            di.title_fr,
            di.owning_collection,
            di.document_type,
            di.publication_year,
            mdv.text_value as num_pages,
            il.en,
            il.fr,
            il.has_translation,
            il.is_translation,
            il.unilingual,
            tmd.*
from stats.v_total_downloads tmd
join stats.dim_item di on di.item_uuid = tmd.item_uuid
join stats.v_item_language il on di.item_uuid = il.item_uuid
left join metadatavalue mdv on mdv.dspace_object_id = di.item_uuid and metadata_field_id = 138
order by total_downloads desc;

-- =========================
-- 7) UTIL: Populate dim_month for a configurable range
--    Adjust the date range below before running.
-- =========================
-- Example: seed months 2015-01 through 2035-12
WITH months AS (
  SELECT generate_series(date '2015-01-01', date '2035-12-01', interval '1 month')::date AS first_day
)
INSERT INTO stats.dim_month (ym_key, year, month, month_name, first_day, last_day)
SELECT (EXTRACT(YEAR  FROM first_day)::int)*100 + (EXTRACT(MONTH FROM first_day)::int) AS ym_key,
       EXTRACT(YEAR  FROM first_day)::int AS year,
       EXTRACT(MONTH FROM first_day)::int AS month,
       TO_CHAR(first_day, 'Month')::text AS month_name,
       first_day,
       (first_day + INTERVAL '1 month - 1 day')::date
FROM months
ON CONFLICT (ym_key) DO NOTHING;

COMMIT;

-- End of file
