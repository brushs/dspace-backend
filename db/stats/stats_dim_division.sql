-- =========================
-- 1) Dimension + Bridge
-- =========================

-- Example configurable entity dimension (UUID PK)
CREATE TABLE IF NOT EXISTS stats.dim_division (
  division_uuid uuid PRIMARY KEY,
  name_en               text,
  name_fr               text,
  sector_en        text,
  sector_fr        text,
  division_code        text
);

-- Bridge from Item -> Config Entity (many-to-many)
CREATE TABLE IF NOT EXISTS stats.br_item_division (
  item_uuid          uuid NOT NULL REFERENCES stats.dim_item(item_uuid) ON DELETE CASCADE,
  division_uuid 	uuid NOT NULL REFERENCES stats.dim_division(division_uuid) ON DELETE CASCADE,
  PRIMARY KEY (item_uuid, division_uuid)
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS ix_br_item_cfg_item  ON stats.br_item_division (item_uuid);
CREATE INDEX IF NOT EXISTS ix_br_item_cfg_cfg   ON stats.br_item_division (division_uuid);

-- =========================
-- 2) RAW MATERIALIZED VIEWS (staging)
--    Replace the placeholders to match your actual DSpace schema.
-- =========================

DROP MATERIALIZED VIEW IF EXISTS stats.mv_division_raw;

CREATE MATERIALIZED VIEW stats.mv_division_raw AS
SELECT
  i.uuid                       AS division_uuid,
  h.handle                     AS handle,
  title_en_md.text_value            AS name_en,
  title_fr_md.text_value            AS name_fr,
  sector_en_md.text_value            AS sector_en,
  sector_fr_md.text_value            AS sector_fr,
  code_md.text_value            AS division_code
FROM item i
JOIN handle h                   ON h.resource_id = i.uuid AND h.resource_type_id = 2
JOIN metadatavalue entity_type_md    ON entity_type_md.dspace_object_id = i.uuid AND entity_type_md.metadata_field_id = 7 and text_value = 'Division'
LEFT JOIN metadatavalue title_en_md   ON title_en_md.dspace_object_id = i.uuid AND title_en_md.metadata_field_id = 73 AND title_en_md.text_lang = 'en'
LEFT JOIN metadatavalue title_fr_md   ON title_fr_md.dspace_object_id = i.uuid AND title_fr_md.metadata_field_id = 73 AND title_fr_md.text_lang = 'fr'
LEFT JOIN metadatavalue sector_en_md   ON sector_en_md.dspace_object_id = i.uuid AND sector_en_md.metadata_field_id = 182 AND sector_en_md.text_lang = 'en'
LEFT JOIN metadatavalue sector_fr_md   ON sector_fr_md.dspace_object_id = i.uuid AND sector_fr_md.metadata_field_id = 182 AND sector_fr_md.text_lang = 'fr'
LEFT JOIN metadatavalue code_md   ON code_md.dspace_object_id = i.uuid AND code_md.metadata_field_id = 180
WHERE i.withdrawn = FALSE;

-- =========================
-- 3) ETL UPSERTS
-- =========================
-- Run Proc, not on create

-- 3.1 Upsert dimension
INSERT INTO stats.dim_division AS d
  (division_uuid, name_en, name_fr, sector_en, sector_fr, division_code)
SELECT DISTINCT ON (r.division_uuid)
  r.division_uuid,
  r.name_en,
  r.name_fr,
  r.sector_en,
  r.sector_fr,
  r.division_code
FROM stats.mv_division_raw r
ON CONFLICT (division_uuid) DO UPDATE
SET name_en        = EXCLUDED.name_en,
    name_fr = EXCLUDED.name_fr,
	sector_en        = EXCLUDED.sector_en,
    sector_fr = EXCLUDED.sector_fr,
	division_code = EXCLUDED.division_code;

-- 3.2 Upsert bridge (only for pairs that resolve to known item & entity)
INSERT INTO stats.br_item_division AS b (item_uuid, division_uuid)
SELECT r.left_id, r.right_id
FROM relationship r
JOIN stats.dim_item di on r.left_id = di.item_uuid
WHERE r.type_id = 16
ON CONFLICT (item_uuid, division_uuid) DO NOTHING;

COMMIT;

-- =========================
-- 4) Reporting view
-- =========================

CREATE OR REPLACE VIEW stats.v_item_division AS
SELECT
  i.item_uuid,
  i.title_en            AS title_en,
  i.title_fr            AS title_fr,
  dce.division_uuid,
  dce.name_en          AS name_en,
  dce.name_fr          AS name_fr,
  dce.sector_en          AS sector_en,
  dce.sector_fr          AS sector_fr,
  dce.division_code   AS division_code
FROM stats.dim_item i
JOIN stats.br_item_division br  ON br.item_uuid = i.item_uuid
JOIN stats.dim_division dce     ON dce.division_uuid = br.division_uuid;
