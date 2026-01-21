CREATE OR REPLACE PROCEDURE stats.run_etl_upserts()
LANGUAGE plpgsql
AS $$
DECLARE
  v_rows bigint;
BEGIN

    REFRESH MATERIALIZED VIEW stats.mv_bitstreams;
    REFRESH MATERIALIZED VIEW stats.mv_division_raw;
    REFRESH MATERIALIZED VIEW stats.mv_item_current_raw;
    REFRESH MATERIALIZED VIEW stats.mv_item_current;
    REFRESH MATERIALIZED VIEW stats.mv_item_externalurl;

  -- 5.1 Upsert items
  INSERT INTO stats.dim_item AS di
    (item_uuid, handle, doi, title_en, title_fr, document_type,
     publication_year, owning_collection, withdrawn)
  SELECT item_uuid, handle, doi, title_en, title_fr, document_type,
         publication_year, owning_collection, withdrawn
  FROM stats.mv_dspace_item_current
  ON CONFLICT (item_uuid) DO UPDATE
  SET handle            = EXCLUDED.handle,
      doi               = EXCLUDED.doi,
      title_en          = EXCLUDED.title_en,
      title_fr          = EXCLUDED.title_fr,
      document_type     = EXCLUDED.document_type,
      publication_year  = EXCLUDED.publication_year,
      owning_collection = EXCLUDED.owning_collection,
      withdrawn         = EXCLUDED.withdrawn;

  GET DIAGNOSTICS v_rows = ROW_COUNT;
  RAISE NOTICE 'dim_item upserted % rows (inserted or updated).', v_rows;

  -- 5.2 Upsert bitstreams
  INSERT INTO stats.dim_bitstream AS db
    (bitstream_uuid, item_uuid, file_name)
  SELECT bitstream_uuid, item_uuid, file_name
  FROM stats.mv_dspace_bitstreams
  ON CONFLICT (bitstream_uuid) DO UPDATE
  SET item_uuid  = EXCLUDED.item_uuid,
      file_name  = EXCLUDED.file_name;

  GET DIAGNOSTICS v_rows = ROW_COUNT;
  RAISE NOTICE 'dim_bitstream upserted % rows.', v_rows;

  -- 5.3 Load pageviews fact from staging
  INSERT INTO stats.fact_item_pageviews_monthly AS f (item_uuid, ym_key, pageviews)
  SELECT s.item_uuid, s.ym_key, SUM(s.pageviews)
  FROM stats.stg_ga_item_monthly s
  GROUP BY s.item_uuid, s.ym_key
  ON CONFLICT (item_uuid, ym_key) DO UPDATE
  SET pageviews = EXCLUDED.pageviews;

  GET DIAGNOSTICS v_rows = ROW_COUNT;
  RAISE NOTICE 'fact_item_pageviews_monthly upserted % rows.', v_rows;

  -- 5.4 Load downloads fact from staging
  INSERT INTO stats.fact_bitstream_downloads_monthly AS f (bitstream_uuid, ym_key, downloads)
  SELECT s.bitstream_uuid, s.ym_key, SUM(s.downloads)
  FROM stats.stg_bitstream_downloads_monthly s
  WHERE exists (SELECT 1 FROM stats.dim_bitstream d WHERE d.bitstream_uuid = s.bitstream_uuid)
  GROUP BY s.bitstream_uuid, s.ym_key
  ON CONFLICT (bitstream_uuid, ym_key) DO UPDATE
  SET downloads = EXCLUDED.downloads;

  GET DIAGNOSTICS v_rows = ROW_COUNT;
  RAISE NOTICE 'fact_bitstream_downloads_monthly upserted % rows.', v_rows;

  INSERT INTO stats.stg_dim_externalurl AS db
    (item_uuid, full_url)
  SELECT item_uuid, full_url
  FROM stats.mv_item_externalurl
  ON CONFLICT (full_url) DO NOTHING;

  UPDATE stats.stg_dim_externalurl
  SET cleaned_filename = full_url;

  UPDATE stats.stg_dim_externalurl
  SET cleaned_filename = regexp_replace(cleaned_filename, '"[^"]*>.*$', '', 'g')
  WHERE cleaned_filename LIKE '%>%';

  UPDATE stats.stg_dim_externalurl
  SET cleaned_filename = REPLACE(cleaned_filename, '<a href="https://cmssostrsharedprod.z9.web.core.windows.net', '');

  UPDATE stats.stg_dim_externalurl
  SET cleaned_filename = REPLACE(cleaned_filename, '<a href="https://ftp.maps.canada.ca', '');

  UPDATE stats.stg_dim_externalurl
  SET cleaned_filename = REPLACE(cleaned_filename, '<a href="https://geoscan.nrcan.gc.ca', '');

  UPDATE stats.stg_dim_externalurl
  SET cleaned_filename = regexp_replace(
      cleaned_filename,
      '^.*https?://[^/]+',  -- match protocol + domain
      '',
      'g'
  )
  WHERE cleaned_filename ~* 'https?://';

  INSERT INTO stats.dim_externalurl
    (cleaned_filename, item_uuid, full_url)
  SELECT cleaned_filename, item_uuid::uuid, full_url
  FROM stats.v_stg_dim_externalurl
  ON CONFLICT (cleaned_filename) DO NOTHING;

    INSERT INTO stats.fact_externalurl_downloads_monthly AS f
        (cleaned_filename, ym_key, downloads)
    SELECT s.cleaned_filename, s.ym_key, SUM(s.downloads)
    FROM stats.stg_externalurl_downloads_monthly s
    WHERE exists (SELECT 1 FROM stats.dim_externalurl d WHERE d.cleaned_filename = s.cleaned_filename)
    GROUP BY s.cleaned_filename, s.ym_key
    ON CONFLICT (cleaned_filename, ym_key) DO UPDATE
    SET downloads = EXCLUDED.downloads;

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


END;
$$;