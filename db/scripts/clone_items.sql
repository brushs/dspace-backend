-- ============================================================
-- DSpace 7.3 Item Clone Script (PostgreSQL / PL/pgSQL)
-- ============================================================
--
-- PURPOSE
--   Clones one or more items in DSpace by reading source UUIDs
--   from the `items_to_clone` staging table.  For each source
--   item the script reproduces:
--     • dspaceobject + item rows
--     • collection2item membership (owning + mapped collections)
--     • metadatavalue rows
--     • resourcepolicy rows
--     • relationship rows (left and right sides)
--     • handle row (optional – explicit handle or auto-minted)
--
--   Bundles and bitstreams are intentionally NOT cloned.
--
-- TWO EXECUTION MODES
--   1. DIRECT (live)  – Call clone_items_batch(TRUE)
--      All clones are written to the database inside individual
--      savepoints so one failure does not abort the whole batch.
--
--   2. SCRIPT (offline) – Call clone_items_batch(FALSE)  **or**
--                         generate_clone_script_to_table()
--      SQL statements are accumulated in `clone_script_output`.
--      Export them to a file with psql:
--
--        \copy (SELECT sql_line
--               FROM   clone_script_output
--               ORDER  BY line_num)
--        TO '/path/to/clone_output.sql'
--
--      Then review and run the file on any target database.
--
-- QUICK-START
--   -- 1. Install
--   \i db/scripts/clone_items.sql
--
--   -- 2. Populate the staging table
--   INSERT INTO items_to_clone
--     (source_item_uuid, target_collection_uuid, new_handle, clone_relationships)
--   VALUES
--     ('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', NULL,        NULL, TRUE),
--     ('ffffffff-0000-1111-2222-333333333333',
--      'cccccccc-dddd-eeee-ffff-000000000000',              -- redirect clone to a different collection
--      '123456789/9999',                                    -- assign this handle to the clone
--      TRUE);
--
--   -- 3a. Execute directly
--   CALL clone_items_batch(TRUE);
--
--   -- 3b. Or generate an SQL script
--   CALL generate_clone_script_to_table();
--   \copy (SELECT sql_line FROM clone_script_output ORDER BY line_num) TO '/tmp/clone.sql'
--   -- review /tmp/clone.sql, then:  psql -f /tmp/clone.sql
--
-- ============================================================


-- ============================================================
-- 0. PREREQUISITES
-- ============================================================
-- Requires PostgreSQL 10+ (gen_random_uuid, ON CONFLICT).
-- Run as the DSpace DB owner (needs INSERT on all relevant tables
-- and USAGE on all sequences).
-- ============================================================


-- ============================================================
-- 1. STAGING TABLE  –  items_to_clone
-- ============================================================
-- Drop and recreate for a clean run, or just use CREATE TABLE IF NOT EXISTS.
-- Adjust as needed; the key columns are source_item_uuid and
-- (optionally) target_collection_uuid.
-- ============================================================

CREATE TABLE IF NOT EXISTS items_to_clone (
                                              id                      SERIAL          PRIMARY KEY,
                                              source_item_uuid        UUID            NOT NULL,
    -- NULL  = keep the same owning collection as the source item
                                              target_collection_uuid  UUID            DEFAULT NULL,
            -- NULL  = auto-mint a handle using handle_seq and inferred prefix
                                              new_handle              VARCHAR(256)    DEFAULT NULL,
    -- TRUE  = also copy relationships (default)
    clone_relationships     BOOLEAN         NOT NULL DEFAULT TRUE,
    -- ---- populated automatically after cloning ----
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    new_item_uuid           UUID            DEFAULT NULL,
    error_message           TEXT            DEFAULT NULL,
    cloned_at               TIMESTAMPTZ     DEFAULT NULL
    );

COMMENT ON TABLE  items_to_clone IS 'Staging table: list items to clone.  Populate before calling clone_items_batch().';
COMMENT ON COLUMN items_to_clone.source_item_uuid       IS 'UUID of the item to copy.';
COMMENT ON COLUMN items_to_clone.target_collection_uuid IS 'Collection for the clone; NULL keeps the same collection as the source.';
COMMENT ON COLUMN items_to_clone.new_handle             IS 'Handle string to register for the clone (e.g. 123456789/999). NULL = auto-mint via handle_seq.';
COMMENT ON COLUMN items_to_clone.clone_relationships    IS 'When TRUE, relationship rows are copied to the clone.';
COMMENT ON COLUMN items_to_clone.status                 IS 'Processing state: PENDING | DONE | ERROR.';
COMMENT ON COLUMN items_to_clone.new_item_uuid          IS 'UUID assigned to the newly created clone (filled after cloning).';


-- ============================================================
-- 2. AUDIT / LOG TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS clone_log (
                                         id                  SERIAL          PRIMARY KEY,
                                         run_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    source_item_uuid    UUID,
    new_item_uuid       UUID,
    status              VARCHAR(20),
    message             TEXT
    );

COMMENT ON TABLE clone_log IS 'Audit trail for clone operations.';


-- ============================================================
-- 3. SCRIPT-OUTPUT TABLE  (used by script-generation mode)
-- ============================================================

CREATE TABLE IF NOT EXISTS clone_script_output (
                                                   line_num    SERIAL  PRIMARY KEY,
                                                   sql_line    TEXT
);

COMMENT ON TABLE clone_script_output IS
  'Holds generated SQL lines from generate_clone_script_to_table(). '
  'Export with: \copy (SELECT sql_line FROM clone_script_output ORDER BY line_num) TO ''/path/to/file.sql''';


-- ============================================================
-- 3b. HANDLE PREFIX HELPER
-- ============================================================
-- Attempts to infer the configured DSpace handle prefix from existing
-- handle rows. If none are found, falls back to 123456789.
-- ============================================================

CREATE OR REPLACE FUNCTION get_default_handle_prefix()
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefix TEXT;
BEGIN
    SELECT split_part(h.handle, '/', 1)
    INTO   v_prefix
    FROM   handle h
    WHERE  position('/' IN h.handle) > 0
    ORDER  BY h.handle_id
    LIMIT  1;

    IF v_prefix IS NULL OR btrim(v_prefix) = '' THEN
        v_prefix := '123456789';
    END IF;

    RETURN v_prefix;
END;
$$;

COMMENT ON FUNCTION get_default_handle_prefix IS
  'Infer default handle prefix from existing handle rows; fallback to 123456789 if none found.';


CREATE OR REPLACE FUNCTION get_item_language_code(
    p_source_uuid UUID
)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_relationship_count INTEGER;
    v_language_uuid      UUID;
BEGIN
    SELECT COUNT(*), MIN(r.right_id)
    INTO   v_relationship_count, v_language_uuid
    FROM   relationship r
    WHERE  r.type_id = 18
      AND  r.left_id = p_source_uuid;

    IF v_relationship_count > 1 THEN
        RAISE EXCEPTION
            'Item % has % language relationships of type_id=18; expected at most one.',
            p_source_uuid,
            v_relationship_count;
    END IF;

    IF v_language_uuid = 'ddf014c4-9d2f-4497-bbd1-1417bf9d5468'::uuid THEN
        RETURN 'fr';
    END IF;

    RETURN 'en';
END;
$$;

COMMENT ON FUNCTION get_item_language_code IS
  'Return fr when the item has a single type_id=18 relationship to ddf014c4-9d2f-4497-bbd1-1417bf9d5468; otherwise en. Raises if more than one type_id=18 relationship exists.';


CREATE OR REPLACE FUNCTION get_opposite_language_right_id(
    p_source_language_code TEXT
)
RETURNS UUID
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_source_language_code = 'en' THEN
        -- French language entity
        RETURN 'ddf014c4-9d2f-4497-bbd1-1417bf9d5468'::uuid;
    END IF;

    -- English language entity (default for source 'fr' or any non-'en')
    RETURN '676fccb3-0b45-41b0-ad49-3eaa3e7e866c'::uuid;
END;
$$;

COMMENT ON FUNCTION get_opposite_language_right_id IS
  'Map source language code to opposite language entity UUID: en->French UUID, otherwise English UUID.';


-- ============================================================
-- 4. CORE CLONING FUNCTION  –  clone_item_direct()
--    Executes the clone in the current transaction.
--    Returns the UUID of the newly created item.
-- ============================================================

CREATE OR REPLACE FUNCTION clone_item_direct(
    p_source_uuid           UUID,
    p_target_collection     UUID    DEFAULT NULL,
    p_new_handle            VARCHAR DEFAULT NULL,
    p_clone_relationships   BOOLEAN DEFAULT TRUE
)
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
v_new_uuid          UUID;
    v_src               RECORD;
    v_coll_uuid         UUID;
    v_mv                RECORD;
    v_rp                RECORD;
    v_rel               RECORD;
    v_c2i               RECORD;
    v_handle_prefix     TEXT;
    v_handle_candidate  VARCHAR(256);
    v_handle_inserted   BOOLEAN := FALSE;
    v_try               INT;
    v_source_handle     VARCHAR(256);
    v_effective_handle  VARCHAR(256);
    v_language_code     TEXT;
    v_new_language_code TEXT;
    v_opposite_language_right_id UUID;
    v_title_field_id    INTEGER;
    v_source_title      TEXT;
    v_new_lang_title    TEXT;
    v_new_title_place   INTEGER;
    v_translation_field_id CONSTANT INTEGER := 282;
    v_source_relation_place INTEGER;
    v_clone_relation_place  INTEGER;
BEGIN
    -- ----------------------------------------------------------
    -- Validate source item exists
    -- ----------------------------------------------------------
SELECT i.uuid,
       i.in_archive,
       i.discoverable,
       i.withdrawn,
       i.last_modified,
       i.owning_collection,
       i.submitter_id
INTO   v_src
FROM   item i
WHERE  i.uuid = p_source_uuid;

IF NOT FOUND THEN
        RAISE EXCEPTION 'clone_item_direct: source item % not found.', p_source_uuid;
END IF;

    -- Prefer non-versioned handle when multiple handles exist for the source item.
    SELECT h.handle
    INTO   v_source_handle
    FROM   handle h
    WHERE  h.resource_id = p_source_uuid
      AND  h.resource_type_id = 2
    ORDER  BY CASE WHEN h.handle !~ '.*/.*\\.[0-9]+$' THEN 0 ELSE 1 END,
              h.handle_id
    LIMIT  1;

    IF v_source_handle IS NULL OR btrim(v_source_handle) = '' THEN
        RAISE EXCEPTION 'clone_item_direct: source item % has no handle.', p_source_uuid;
    END IF;

    v_language_code := get_item_language_code(p_source_uuid);
    v_new_language_code := CASE WHEN v_language_code = 'en' THEN 'fr' ELSE 'en' END;
    v_opposite_language_right_id := get_opposite_language_right_id(v_language_code);

    SELECT mf.metadata_field_id
    INTO   v_title_field_id
    FROM   metadatafieldregistry mf
    JOIN   metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id
    WHERE  ms.short_id = 'dc'
      AND  mf.element = 'title'
      AND  mf.qualifier IS NULL
    LIMIT 1;

    IF v_title_field_id IS NULL THEN
        RAISE EXCEPTION 'clone_item_direct: dc.title metadata field was not found in metadatafieldregistry.';
    END IF;

    -- Source item must have a title in its own language.
    SELECT mv.text_value
    INTO   v_source_title
    FROM   metadatavalue mv
    WHERE  mv.dspace_object_id = p_source_uuid
      AND  mv.metadata_field_id = v_title_field_id
      AND  mv.text_lang = v_language_code
    ORDER  BY mv.place
    LIMIT 1;

    IF v_source_title IS NULL OR btrim(v_source_title) = '' THEN
        RAISE EXCEPTION
            'clone_item_direct: source item % has no dc.title value for language %.',
            p_source_uuid,
            v_language_code;
    END IF;

    -- ----------------------------------------------------------
    -- Allocate new UUID and resolve target collection
    -- ----------------------------------------------------------
    v_new_uuid  := gen_random_uuid();
    v_coll_uuid := COALESCE(p_target_collection, v_src.owning_collection);

    -- ----------------------------------------------------------
    -- 1. dspaceobject  (parent table)
    -- ----------------------------------------------------------
INSERT INTO dspaceobject (uuid)
VALUES (v_new_uuid);

-- ----------------------------------------------------------
-- 2. item
-- ----------------------------------------------------------
INSERT INTO item (
    uuid,
    in_archive,
    discoverable,
    withdrawn,
    last_modified,
    owning_collection,
    submitter_id
)
VALUES (
           v_new_uuid,
           v_src.in_archive,
           v_src.discoverable,
           v_src.withdrawn,
           NOW(),
           v_coll_uuid,
           v_src.submitter_id
       );

-- ----------------------------------------------------------
-- 3. collection2item – owning collection
-- ----------------------------------------------------------
INSERT INTO collection2item (collection_id, item_id)
VALUES (v_coll_uuid, v_new_uuid)
    ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------
-- 4. collection2item – additional mapped collections
--    (all collections the source item belonged to except the
--     new owning collection, to avoid a duplicate)
-- ----------------------------------------------------------
FOR v_c2i IN
SELECT c2i.collection_id
FROM   collection2item c2i
WHERE  c2i.item_id       = p_source_uuid
  AND  c2i.collection_id <> v_coll_uuid
    LOOP
INSERT INTO collection2item (collection_id, item_id)
VALUES (v_c2i.collection_id, v_new_uuid)
ON CONFLICT DO NOTHING;
END LOOP;

    -- ----------------------------------------------------------
    -- 5. metadatavalue
    -- ----------------------------------------------------------
INSERT INTO metadatavalue (
    metadata_field_id,
    text_value,
    text_lang,
    place,
    authority,
    confidence,
    dspace_object_id
)
SELECT
    mv.metadata_field_id,
    mv.text_value,
    mv.text_lang,
    mv.place,
    mv.authority,
    mv.confidence,
    v_new_uuid
FROM   metadatavalue mv
WHERE  mv.dspace_object_id = p_source_uuid
  AND  mv.metadata_field_id NOT IN (32,73,275,27);
-- exclude fields DOI, Title, CATN and ISBN

-- ----------------------------------------------------------
-- 6. resourcepolicy
-- ----------------------------------------------------------
INSERT INTO resourcepolicy (
    dspace_object,
    resource_type_id,
    action_id,
    eperson_id,
    epersongroup_id,
    start_date,
    end_date,
    rpname,
    rptype,
    rpdescription,
    policy_id
)
SELECT
    v_new_uuid,
    rp.resource_type_id,
    rp.action_id,
    rp.eperson_id,
    rp.epersongroup_id,
    rp.start_date,
    rp.end_date,
    rp.rpname,
    rp.rptype,
    rp.rpdescription,
    nextval('resourcepolicy_seq')
FROM   resourcepolicy rp
WHERE  rp.dspace_object = p_source_uuid;

-- ----------------------------------------------------------
-- 7. relationships  (optional)
--    The clone inherits every relationship of the source.
--    Where the source was on the LEFT  → clone becomes left_id.
--    Where the source was on the RIGHT → clone becomes right_id.
--    ON CONFLICT DO NOTHING guards against the unique
--    constraint (left_id, type_id, right_id).
-- ----------------------------------------------------------
IF p_clone_relationships THEN

        -- Source item was on the LEFT
        FOR v_rel IN
SELECT r.type_id,
       r.right_id,
       r.left_place,
       r.right_place,
       r.leftward_value,
       r.rightward_value,
       r.latest_version_status
FROM   relationship r
WHERE  r.left_id = p_source_uuid
  AND  r.type_id <> 18
    LOOP
INSERT INTO relationship (
    id,
    left_id,
    type_id,
    right_id,
    left_place,
    right_place,
    leftward_value,
    rightward_value,
    latest_version_status
)
VALUES (
    nextval('relationship_id_seq'),
    v_new_uuid,
    v_rel.type_id,
    v_rel.right_id,
    v_rel.left_place,
    v_rel.right_place,
    v_rel.leftward_value,
    v_rel.rightward_value,
    v_rel.latest_version_status
    )
ON CONFLICT DO NOTHING;
END LOOP;

        -- Source item was on the RIGHT
FOR v_rel IN
SELECT r.left_id,
       r.type_id,
       r.left_place,
       r.right_place,
       r.leftward_value,
       r.rightward_value,
       r.latest_version_status
FROM   relationship r
WHERE  r.right_id = p_source_uuid
  AND  r.type_id <> 18
    LOOP
INSERT INTO relationship (
    id,
    left_id,
    type_id,
    right_id,
    left_place,
    right_place,
    leftward_value,
    rightward_value,
    latest_version_status
)
VALUES (
    nextval('relationship_id_seq'),
    v_rel.left_id,
    v_rel.type_id,
    v_new_uuid,
    v_rel.left_place,
    v_rel.right_place,
    v_rel.leftward_value,
    v_rel.rightward_value,
    v_rel.latest_version_status
    )
ON CONFLICT DO NOTHING;
END LOOP;

END IF;

    -- 8.1 Insert opposite language relationship (type_id=18) for cloned item.
    --     Keep exactly one relationship for this clone by replacing any existing type_id=18 row.
    DELETE FROM relationship
    WHERE left_id = v_new_uuid
      AND type_id = 18;

    INSERT INTO relationship (
        id,
        left_id,
        type_id,
        right_id,
        left_place,
        right_place,
        latest_version_status
    )
    VALUES (
        nextval('relationship_id_seq'),
        v_new_uuid,
        18,
        v_opposite_language_right_id,
        0,
        0,
        0
    );

    -- ----------------------------------------------------------
    -- 8. handle
    --    resource_type_id = 2  →  Constants.ITEM
    --    If p_new_handle is NULL, auto-mint using:
    --      inferred_prefix || '/' || nextval('handle_seq')
    -- ----------------------------------------------------------
    IF p_new_handle IS NOT NULL THEN
        v_effective_handle := p_new_handle;
        INSERT INTO handle (
            handle_id,
            handle,
            resource_id,
            resource_type_id
        )
        VALUES (
            nextval('handle_id_seq'),
            p_new_handle,
            v_new_uuid,
            2
        );
    ELSE
        v_handle_prefix := get_default_handle_prefix();

        -- Retry a few times in case handle_seq is out-of-sync.
        FOR v_try IN 1..20 LOOP
            v_handle_candidate := v_handle_prefix || '/' || nextval('handle_seq')::text;

            BEGIN
                INSERT INTO handle (
                    handle_id,
                    handle,
                    resource_id,
                    resource_type_id
                )
                VALUES (
                    nextval('handle_id_seq'),
                    v_handle_candidate,
                    v_new_uuid,
                    2
                );

                v_handle_inserted := TRUE;
                v_effective_handle := v_handle_candidate;
                EXIT;
            EXCEPTION
                WHEN unique_violation THEN
                    NULL;
            END;
        END LOOP;

        IF NOT v_handle_inserted THEN
            RAISE EXCEPTION
                'clone_item_direct: unable to auto-generate unique handle for % after % attempts (prefix=%).',
                v_new_uuid, 20, v_handle_prefix;
        END IF;
END IF;

    UPDATE metadatavalue mv
    SET    text_value = REPLACE(text_value, v_source_handle, v_effective_handle)
    WHERE  mv.dspace_object_id = v_new_uuid
      AND  mv.metadata_field_id = 34;

    -- 10. Ensure the cloned item has a title in its related language.
    SELECT mv.text_value
    INTO   v_new_lang_title
    FROM   metadatavalue mv
    WHERE  mv.dspace_object_id = v_new_uuid
      AND  mv.metadata_field_id = v_title_field_id
      AND  mv.text_lang = v_new_language_code
    ORDER  BY mv.place
    LIMIT 1;

    IF v_new_lang_title IS NULL OR btrim(v_new_lang_title) = '' THEN
        SELECT COALESCE(MAX(mv.place), -1) + 1
        INTO   v_new_title_place
        FROM   metadatavalue mv
        WHERE  mv.dspace_object_id = v_new_uuid
          AND  mv.metadata_field_id = v_title_field_id
          AND  mv.text_lang = v_new_language_code;

        INSERT INTO metadatavalue (
            metadata_value_id,
            metadata_field_id,
            text_value,
            text_lang,
            place,
            authority,
            confidence,
            dspace_object_id
        )
        VALUES (
            nextval('metadatavalue_seq'),
            v_title_field_id,
            '_' || v_source_title,
            v_new_language_code,
            v_new_title_place,
            NULL,
            -1,
            v_new_uuid
        );

        v_new_lang_title := '_' || v_source_title;
    END IF;

    -- 11. Maintain dc.relation.istranslationof for both source and cloned items.
    --     Each item points to the other item's handle and displays the other item's title in the other language.

    -- Source item links to cloned item using cloned opposite-language title.
    -- Add same value twice (en/fr), changing only text_lang.
    DELETE FROM metadatavalue
    WHERE dspace_object_id = p_source_uuid
      AND metadata_field_id = v_translation_field_id
      AND text_lang IN ('en', 'fr');

    SELECT COALESCE(MAX(mv.place), -1) + 1
    INTO   v_source_relation_place
    FROM   metadatavalue mv
    WHERE  mv.dspace_object_id = p_source_uuid
      AND  mv.metadata_field_id = v_translation_field_id
      AND  mv.text_lang IN ('en', 'fr');

    INSERT INTO metadatavalue (
        metadata_value_id,
        metadata_field_id,
        text_value,
        text_lang,
        place,
        authority,
        confidence,
        dspace_object_id
    )
    VALUES (
        nextval('metadatavalue_seq'),
        v_translation_field_id,
        '<a href="https://ostrnrcan-dostrncan.canada.ca/handle/' || v_effective_handle || '">' || v_new_lang_title || '</a>',
        'en',
        v_source_relation_place,
        NULL,
        -1,
        p_source_uuid
    );

    INSERT INTO metadatavalue (
        metadata_value_id,
        metadata_field_id,
        text_value,
        text_lang,
        place,
        authority,
        confidence,
        dspace_object_id
    )
    VALUES (
        nextval('metadatavalue_seq'),
        v_translation_field_id,
        '<a href="https://ostrnrcan-dostrncan.canada.ca/handle/' || v_effective_handle || '">' || v_new_lang_title || '</a>',
        'fr',
        v_source_relation_place + 1,
        NULL,
        -1,
        p_source_uuid
    );

    -- Cloned item links to source item using source-language title.
    -- Add same value twice (en/fr), changing only text_lang.
    DELETE FROM metadatavalue
    WHERE dspace_object_id = v_new_uuid
      AND metadata_field_id = v_translation_field_id
      AND text_lang IN ('en', 'fr');

    SELECT COALESCE(MAX(mv.place), -1) + 1
    INTO   v_clone_relation_place
    FROM   metadatavalue mv
    WHERE  mv.dspace_object_id = v_new_uuid
      AND  mv.metadata_field_id = v_translation_field_id
      AND  mv.text_lang IN ('en', 'fr');

    INSERT INTO metadatavalue (
        metadata_value_id,
        metadata_field_id,
        text_value,
        text_lang,
        place,
        authority,
        confidence,
        dspace_object_id
    )
    VALUES (
        nextval('metadatavalue_seq'),
        v_translation_field_id,
        '<a href="https://ostrnrcan-dostrncan.canada.ca/handle/' || v_source_handle || '">' || v_source_title || '</a>',
        'en',
        v_clone_relation_place,
        NULL,
        -1,
        v_new_uuid
    );

    INSERT INTO metadatavalue (
        metadata_value_id,
        metadata_field_id,
        text_value,
        text_lang,
        place,
        authority,
        confidence,
        dspace_object_id
    )
    VALUES (
        nextval('metadatavalue_seq'),
        v_translation_field_id,
        '<a href="https://ostrnrcan-dostrncan.canada.ca/handle/' || v_source_handle || '">' || v_source_title || '</a>',
        'fr',
        v_clone_relation_place + 1,
        NULL,
        -1,
        v_new_uuid
    );

RETURN v_new_uuid;
END;
$$;

COMMENT ON FUNCTION clone_item_direct IS
  'Clone a single DSpace item (metadata, policies, relationships). '
  'Returns the new item UUID.  Does NOT clone bundles or bitstreams.';


-- ============================================================
-- 5. SQL-GENERATION FUNCTION  –  generate_clone_sql()
--    Returns a SETOF TEXT: one SQL statement per row.
--    The caller can store, review, and execute these later.
--    Uses gen_random_uuid() at call-time so the UUID is
--    embedded in the generated SQL (stable, no guessing).
-- ============================================================

CREATE OR REPLACE FUNCTION generate_clone_sql(
    p_source_uuid           UUID,
    p_target_collection     UUID    DEFAULT NULL,
    p_new_handle            VARCHAR DEFAULT NULL,
    p_clone_relationships   BOOLEAN DEFAULT TRUE
)
RETURNS SETOF TEXT
LANGUAGE plpgsql
AS $$
DECLARE
v_new_uuid      UUID;
    v_src           RECORD;
    v_coll_uuid     UUID;
    v_mv            RECORD;
    v_rp            RECORD;
    v_rel           RECORD;
    v_c2i           RECORD;
    v_mv_count      BIGINT;
    v_rp_count      BIGINT;
    v_handle_prefix TEXT;
    v_source_handle TEXT;
    v_language_code TEXT;
    v_new_language_code TEXT;
    v_opposite_language_right_id UUID;
    v_title_field_id INTEGER;
    v_source_title TEXT;
    v_has_new_lang_title BOOLEAN;
    v_other_language_source_title TEXT;
    v_clone_title_for_source_link TEXT;
    v_translation_field_id CONSTANT INTEGER := 282;
    v_new_handle_sql_expr TEXT;
BEGIN
    -- ----------------------------------------------------------
    -- Validate
    -- ----------------------------------------------------------
SELECT i.uuid,
       i.in_archive,
       i.discoverable,
       i.withdrawn,
       i.last_modified,
       i.owning_collection,
       i.submitter_id
INTO   v_src
FROM   item i
WHERE  i.uuid = p_source_uuid;

IF NOT FOUND THEN
        RAISE EXCEPTION 'generate_clone_sql: source item % not found.', p_source_uuid;
END IF;

    v_new_uuid  := gen_random_uuid();
    v_coll_uuid := COALESCE(p_target_collection, v_src.owning_collection);

SELECT COUNT(*) INTO v_mv_count FROM metadatavalue  WHERE dspace_object_id = p_source_uuid;
SELECT COUNT(*) INTO v_rp_count FROM resourcepolicy WHERE dspace_object     = p_source_uuid;

    SELECT h.handle
    INTO   v_source_handle
    FROM   handle h
    WHERE  h.resource_id = p_source_uuid
      AND  h.resource_type_id = 2
    ORDER  BY CASE WHEN h.handle !~ '.*/.*\\.[0-9]+$' THEN 0 ELSE 1 END,
              h.handle_id
    LIMIT  1;

    IF v_source_handle IS NULL OR btrim(v_source_handle) = '' THEN
        RAISE EXCEPTION 'generate_clone_sql: source item % has no handle.', p_source_uuid;
    END IF;

    v_language_code := get_item_language_code(p_source_uuid);
    v_new_language_code := CASE WHEN v_language_code = 'en' THEN 'fr' ELSE 'en' END;
    v_opposite_language_right_id := get_opposite_language_right_id(v_language_code);

    SELECT mf.metadata_field_id
    INTO   v_title_field_id
    FROM   metadatafieldregistry mf
    JOIN   metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id
    WHERE  ms.short_id = 'dc'
      AND  mf.element = 'title'
      AND  mf.qualifier IS NULL
    LIMIT 1;

    IF v_title_field_id IS NULL THEN
        RAISE EXCEPTION 'generate_clone_sql: dc.title metadata field was not found in metadatafieldregistry.';
    END IF;

    SELECT mv.text_value
    INTO   v_source_title
    FROM   metadatavalue mv
    WHERE  mv.dspace_object_id = p_source_uuid
      AND  mv.metadata_field_id = v_title_field_id
      AND  mv.text_lang = v_language_code
    ORDER  BY mv.place
    LIMIT 1;

    IF v_source_title IS NULL OR btrim(v_source_title) = '' THEN
        RAISE EXCEPTION
            'generate_clone_sql: source item % has no dc.title value for language %.',
            p_source_uuid,
            v_language_code;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM metadatavalue mv
        WHERE mv.dspace_object_id = p_source_uuid
          AND mv.metadata_field_id = v_title_field_id
          AND mv.text_lang = v_new_language_code
          AND btrim(COALESCE(mv.text_value, '')) <> ''
    )
    INTO v_has_new_lang_title;

    SELECT mv.text_value
    INTO   v_other_language_source_title
    FROM   metadatavalue mv
    WHERE  mv.dspace_object_id = p_source_uuid
      AND  mv.metadata_field_id = v_title_field_id
      AND  mv.text_lang = v_new_language_code
    ORDER  BY mv.place
    LIMIT 1;

    v_clone_title_for_source_link := COALESCE(v_other_language_source_title, '_' || v_source_title);

-- ----------------------------------------------------------
-- Header comment
-- ----------------------------------------------------------
RETURN NEXT '-- =============================================';
    RETURN NEXT format('-- Clone of source item : %s', p_source_uuid);
    RETURN NEXT format('-- New item UUID        : %s', v_new_uuid);
    RETURN NEXT format('-- Target collection    : %s', v_coll_uuid);
    RETURN NEXT format('-- Handle               : %s', COALESCE(p_new_handle, '(none)'));
    RETURN NEXT format('-- Clone relationships  : %s', p_clone_relationships);
    RETURN NEXT format('-- Metadata values      : %s', v_mv_count);
    RETURN NEXT format('-- Resource policies    : %s', v_rp_count);
    RETURN NEXT format('-- Generated            : %s', NOW());
    RETURN NEXT '-- =============================================';
    RETURN NEXT '';

    -- ----------------------------------------------------------
    -- 1. dspaceobject
    -- ----------------------------------------------------------
    RETURN NEXT '-- 1. dspaceobject';
    RETURN NEXT format(
        'INSERT INTO dspaceobject (uuid) VALUES (%L);',
        v_new_uuid
    );
    RETURN NEXT '';

    -- ----------------------------------------------------------
    -- 2. item
    -- ----------------------------------------------------------
    RETURN NEXT '-- 2. item';
    RETURN NEXT format(
        'INSERT INTO item'
        ' (uuid, in_archive, discoverable, withdrawn, last_modified, owning_collection, submitter_id)'
        ' VALUES (%L, %L, %L, %L, NOW(), %L, %L);',
        v_new_uuid,
        v_src.in_archive,
        v_src.discoverable,
        v_src.withdrawn,
        v_coll_uuid,
        v_src.submitter_id
    );
    RETURN NEXT '';

    -- ----------------------------------------------------------
    -- 3 & 4. collection2item
    -- ----------------------------------------------------------
    RETURN NEXT '-- 3-4. collection2item (owning + mapped collections)';
    RETURN NEXT format(
        'INSERT INTO collection2item (collection_id, item_id) VALUES (%L, %L) ON CONFLICT DO NOTHING;',
        v_coll_uuid, v_new_uuid
    );

FOR v_c2i IN
SELECT c2i.collection_id
FROM   collection2item c2i
WHERE  c2i.item_id       = p_source_uuid
  AND  c2i.collection_id <> v_coll_uuid
    LOOP
        RETURN NEXT format(
            'INSERT INTO collection2item (collection_id, item_id) VALUES (%L, %L) ON CONFLICT DO NOTHING;',
            v_c2i.collection_id, v_new_uuid
        );
END LOOP;
    RETURN NEXT '';

    -- ----------------------------------------------------------
    -- 5. metadatavalue
    -- ----------------------------------------------------------
    RETURN NEXT format('-- 5. metadata values (%s rows)', v_mv_count);
FOR v_mv IN
SELECT mv.metadata_field_id,
       mv.text_value,
       mv.text_lang,
       mv.place,
       mv.authority,
       mv.confidence
FROM   metadatavalue mv
WHERE  mv.dspace_object_id = p_source_uuid
  AND  mv.metadata_field_id <> 32
ORDER  BY mv.metadata_field_id, mv.place
    LOOP
        RETURN NEXT format(
            'INSERT INTO metadatavalue'
            ' (metadata_value_id, metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id)'
            ' VALUES (nextval(''metadatavalue_seq''), %L, %L, %L, %L, %L, %L, %L);',
            v_mv.metadata_field_id,
            v_mv.text_value,
            v_mv.text_lang,
            v_mv.place,
            v_mv.authority,
            v_mv.confidence,
            v_new_uuid
        );
END LOOP;
    RETURN NEXT '';

    -- ----------------------------------------------------------
    -- 6. resourcepolicy
    -- ----------------------------------------------------------
    RETURN NEXT format('-- 6. resource policies (%s rows)', v_rp_count);
FOR v_rp IN
SELECT rp.resource_type_id,
       rp.action_id,
       rp.eperson_id,
       rp.epersongroup_id,
       rp.start_date,
       rp.end_date,
       rp.rpname,
       rp.rptype,
       rp.rpdescription
FROM   resourcepolicy rp
WHERE  rp.dspace_object = p_source_uuid
    LOOP
        RETURN NEXT format(
            'INSERT INTO resourcepolicy'
            ' (policy_id, dspace_object, resource_type_id, action_id,'
            '  eperson_id, epersongroup_id, start_date, end_date, rpname, rptype, rpdescription)'
            ' VALUES (nextval(''resourcepolicy_seq''), %L, %L, %L, %L, %L, %L, %L, %L, %L, %L);',
            v_new_uuid,
            v_rp.resource_type_id,
            v_rp.action_id,
            v_rp.eperson_id,
            v_rp.epersongroup_id,
            v_rp.start_date,
            v_rp.end_date,
            v_rp.rpname,
            v_rp.rptype,
            v_rp.rpdescription
        );
END LOOP;
    RETURN NEXT '';

    -- ----------------------------------------------------------
    -- 7. relationships
    -- ----------------------------------------------------------
    IF p_clone_relationships THEN
        RETURN NEXT '-- 7. relationships';

        -- Source was LEFT
FOR v_rel IN
SELECT r.type_id,
       r.right_id,
       r.left_place,
       r.right_place,
       r.leftward_value,
       r.rightward_value,
       r.latest_version_status
FROM   relationship r
WHERE  r.left_id = p_source_uuid
  AND  r.type_id <> 18
    LOOP
            RETURN NEXT format(
                'INSERT INTO relationship'
                ' (id, left_id, type_id, right_id, left_place, right_place,'
                '  leftward_value, rightward_value, latest_version_status)'
                ' VALUES (nextval(''relationship_id_seq''), %L, %L, %L, %L, %L, %L, %L, %L)'
                ' ON CONFLICT DO NOTHING;',
                v_new_uuid,
                v_rel.type_id,
                v_rel.right_id,
                v_rel.left_place,
                v_rel.right_place,
                v_rel.leftward_value,
                v_rel.rightward_value,
                v_rel.latest_version_status
            );
END LOOP;

        -- Source was RIGHT
FOR v_rel IN
SELECT r.left_id,
       r.type_id,
       r.left_place,
       r.right_place,
       r.leftward_value,
       r.rightward_value,
       r.latest_version_status
FROM   relationship r
WHERE  r.right_id = p_source_uuid
  AND  r.type_id <> 18
    LOOP
            RETURN NEXT format(
                'INSERT INTO relationship'
                ' (id, left_id, type_id, right_id, left_place, right_place,'
                '  leftward_value, rightward_value, latest_version_status)'
                ' VALUES (nextval(''relationship_id_seq''), %L, %L, %L, %L, %L, %L, %L, %L)'
                ' ON CONFLICT DO NOTHING;',
                v_rel.left_id,
                v_rel.type_id,
                v_new_uuid,
                v_rel.left_place,
                v_rel.right_place,
                v_rel.leftward_value,
                v_rel.rightward_value,
                v_rel.latest_version_status
            );
END LOOP;

        RETURN NEXT '';
END IF;

    -- ----------------------------------------------------------
    -- 8. handle
    -- ----------------------------------------------------------
    IF p_new_handle IS NOT NULL THEN
        v_new_handle_sql_expr := quote_literal(p_new_handle);
        RETURN NEXT '-- 8. handle';
        RETURN NEXT format(
            'INSERT INTO handle (handle_id, handle, resource_id, resource_type_id)'
            ' VALUES (nextval(''handle_id_seq''), %L, %L, 2);',
            p_new_handle, v_new_uuid
        );
        RETURN NEXT '';

        IF v_source_handle IS NOT NULL AND v_source_handle <> p_new_handle THEN
            RETURN NEXT '-- 9. rewrite cloned dc.identifier handle values';
            RETURN NEXT format(
                'UPDATE metadatavalue mv SET text_value = %L WHERE mv.dspace_object_id = %L AND mv.text_value = %L AND mv.metadata_field_id IN '
                || '(SELECT mf.metadata_field_id FROM metadatafieldregistry mf JOIN metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id WHERE ms.short_id = ''dc'' AND mf.element = ''identifier'');',
                p_new_handle,
                v_new_uuid,
                v_source_handle
            );
            RETURN NEXT format(
                'UPDATE metadatavalue mv SET text_value = %L WHERE mv.dspace_object_id = %L AND mv.text_value = %L AND mv.metadata_field_id IN '
                || '(SELECT mf.metadata_field_id FROM metadatafieldregistry mf JOIN metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id WHERE ms.short_id = ''dc'' AND mf.element = ''identifier'');',
                'http://hdl.handle.net/' || p_new_handle,
                v_new_uuid,
                'http://hdl.handle.net/' || v_source_handle
            );
            RETURN NEXT format(
                'UPDATE metadatavalue mv SET text_value = %L WHERE mv.dspace_object_id = %L AND mv.text_value = %L AND mv.metadata_field_id IN '
                || '(SELECT mf.metadata_field_id FROM metadatafieldregistry mf JOIN metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id WHERE ms.short_id = ''dc'' AND mf.element = ''identifier'');',
                'https://hdl.handle.net/' || p_new_handle,
                v_new_uuid,
                'https://hdl.handle.net/' || v_source_handle
            );
            RETURN NEXT '';
        END IF;
    ELSE
        v_handle_prefix := get_default_handle_prefix();
        v_new_handle_sql_expr := quote_literal(v_handle_prefix || '/') || ' || currval(''handle_seq'')';
        RETURN NEXT '-- 8. handle (auto-generated at execution time from handle_seq)';
        RETURN NEXT
            'INSERT INTO handle (handle_id, handle, resource_id, resource_type_id) VALUES '
            || '(nextval(''handle_id_seq''), '
            || quote_literal(v_handle_prefix || '/')
            || ' || nextval(''handle_seq''), '
            || quote_literal(v_new_uuid::text)
            || ', 2);';
        RETURN NEXT '';

        IF v_source_handle IS NOT NULL THEN
            RETURN NEXT '-- 9. rewrite cloned dc.identifier handle values (auto-minted handle)';
            RETURN NEXT
                'UPDATE metadatavalue mv SET text_value = '
                || quote_literal(v_handle_prefix || '/')
                || ' || currval(''handle_seq'') WHERE mv.dspace_object_id = '
                || quote_literal(v_new_uuid::text)
                || ' AND mv.text_value = '
                || quote_literal(v_source_handle)
                || ' AND mv.metadata_field_id IN '
                || '(SELECT mf.metadata_field_id FROM metadatafieldregistry mf JOIN metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id WHERE ms.short_id = ''dc'' AND mf.element = ''identifier'');';

            RETURN NEXT
                'UPDATE metadatavalue mv SET text_value = '
                || quote_literal('http://hdl.handle.net/' || v_handle_prefix || '/')
                || ' || currval(''handle_seq'') WHERE mv.dspace_object_id = '
                || quote_literal(v_new_uuid::text)
                || ' AND mv.text_value = '
                || quote_literal('http://hdl.handle.net/' || v_source_handle)
                || ' AND mv.metadata_field_id IN '
                || '(SELECT mf.metadata_field_id FROM metadatafieldregistry mf JOIN metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id WHERE ms.short_id = ''dc'' AND mf.element = ''identifier'');';

            RETURN NEXT
                'UPDATE metadatavalue mv SET text_value = '
                || quote_literal('https://hdl.handle.net/' || v_handle_prefix || '/')
                || ' || currval(''handle_seq'') WHERE mv.dspace_object_id = '
                || quote_literal(v_new_uuid::text)
                || ' AND mv.text_value = '
                || quote_literal('https://hdl.handle.net/' || v_source_handle)
                || ' AND mv.metadata_field_id IN '
                || '(SELECT mf.metadata_field_id FROM metadatafieldregistry mf JOIN metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id WHERE ms.short_id = ''dc'' AND mf.element = ''identifier'');';
            RETURN NEXT '';
        END IF;
END IF;

    RETURN NEXT '-- 7.1 opposite language relationship (type_id=18)';
    RETURN NEXT format('-- Source language: %s, opposite language right_id: %s', v_language_code, v_opposite_language_right_id);
    RETURN NEXT format('DELETE FROM relationship WHERE left_id = %L AND type_id = 18;', v_new_uuid);
    RETURN NEXT format(
        'INSERT INTO relationship (id, left_id, type_id, right_id, left_place, right_place, latest_version_status) VALUES (nextval(''relationship_id_seq''), %L, 18, %L, 0, 0, 0);',
        v_new_uuid,
        v_opposite_language_right_id
    );
    RETURN NEXT '';

    RETURN NEXT format('-- Derived item language from relationship type_id=18: %s', v_language_code);
    RETURN NEXT '';

    IF NOT v_has_new_lang_title THEN
        RETURN NEXT '-- 10. add missing opposite-language dc.title to clone (prefixed with _)';
        RETURN NEXT format(
            'INSERT INTO metadatavalue (metadata_value_id, metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id)'
            ' SELECT nextval(''metadatavalue_seq''), %s, %L, %L, COALESCE(MAX(mv.place), -1) + 1, NULL, -1, %L'
            ' FROM metadatavalue mv'
            ' WHERE mv.dspace_object_id = %L AND mv.metadata_field_id = %s AND mv.text_lang = %L;',
            v_title_field_id,
            '_' || v_source_title,
            v_new_language_code,
            v_new_uuid,
            v_new_uuid,
            v_title_field_id,
            v_new_language_code
        );
        RETURN NEXT '';
    END IF;

    RETURN NEXT '-- 11. maintain dc.relation.istranslationof (metadata_field_id=282) for source and clone';
    RETURN NEXT format(
        'DELETE FROM metadatavalue WHERE dspace_object_id = %L AND metadata_field_id = %s AND text_lang IN (''en'',''fr'');',
        p_source_uuid,
        v_translation_field_id
    );
    RETURN NEXT
        'INSERT INTO metadatavalue (metadata_value_id, metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id) '
        || 'SELECT nextval(''metadatavalue_seq''), '
        || v_translation_field_id::text
        || ', ''<a href="https://ostrnrcan-dostrncan.canada.ca/handle/'' || '
        || v_new_handle_sql_expr
        || ' || ''">'
        || replace(v_clone_title_for_source_link, '''', '''''')
        || '</a>'', '
        || '''en'''
        || ', COALESCE(MAX(mv.place), -1) + 1, NULL, -1, '
        || quote_literal(p_source_uuid::text)
        || ' FROM metadatavalue mv WHERE mv.dspace_object_id = '
        || quote_literal(p_source_uuid::text)
        || ' AND mv.metadata_field_id = '
        || v_translation_field_id::text
        || ' AND mv.text_lang IN (''en'',''fr'')'
        || ';';

    RETURN NEXT
        'INSERT INTO metadatavalue (metadata_value_id, metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id) '
        || 'SELECT nextval(''metadatavalue_seq''), '
        || v_translation_field_id::text
        || ', ''<a href="https://ostrnrcan-dostrncan.canada.ca/handle/'' || '
        || v_new_handle_sql_expr
        || ' || ''">'
        || replace(v_clone_title_for_source_link, '''', '''''')
        || '</a>'', ''fr'', COALESCE(MAX(mv.place), -1) + 2, NULL, -1, '
        || quote_literal(p_source_uuid::text)
        || ' FROM metadatavalue mv WHERE mv.dspace_object_id = '
        || quote_literal(p_source_uuid::text)
        || ' AND mv.metadata_field_id = '
        || v_translation_field_id::text
        || ' AND mv.text_lang IN (''en'',''fr'')'
        || ';';

    RETURN NEXT format(
        'DELETE FROM metadatavalue WHERE dspace_object_id = %L AND metadata_field_id = %s AND text_lang IN (''en'',''fr'');',
        v_new_uuid,
        v_translation_field_id
    );
    RETURN NEXT format(
        'INSERT INTO metadatavalue (metadata_value_id, metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id)'
        ' SELECT nextval(''metadatavalue_seq''), %s,'
        ' %L, %L, COALESCE(MAX(mv.place), -1) + 1, NULL, -1, %L'
        ' FROM metadatavalue mv'
        ' WHERE mv.dspace_object_id = %L AND mv.metadata_field_id = %s AND mv.text_lang IN (''en'',''fr'');',
        v_translation_field_id,
        '<a href="https://ostrnrcan-dostrncan.canada.ca/handle/' || v_source_handle || '">' || v_source_title || '</a>',
        'en',
        v_new_uuid,
        v_new_uuid,
        v_translation_field_id
    );
    RETURN NEXT format(
        'INSERT INTO metadatavalue (metadata_value_id, metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id)'
        ' SELECT nextval(''metadatavalue_seq''), %s,'
        ' %L, ''fr'', COALESCE(MAX(mv.place), -1) + 2, NULL, -1, %L'
        ' FROM metadatavalue mv'
        ' WHERE mv.dspace_object_id = %L AND mv.metadata_field_id = %s AND mv.text_lang IN (''en'',''fr'');',
        v_translation_field_id,
        '<a href="https://ostrnrcan-dostrncan.canada.ca/handle/' || v_source_handle || '">' || v_source_title || '</a>',
        v_new_uuid,
        v_new_uuid,
        v_translation_field_id
    );
    RETURN NEXT '';

    RETURN NEXT format('-- End clone %s  →  %s', p_source_uuid, v_new_uuid);
    RETURN NEXT '';
END;
$$;

COMMENT ON FUNCTION generate_clone_sql IS
  'Returns a set of SQL TEXT lines that, when executed, reproduce '
  'the given item (metadata, policies, relationships, optional handle). '
  'Does NOT execute anything itself – safe to call in read-only transactions.';


-- ============================================================
-- 6. BATCH PROCEDURE  –  clone_items_batch()
--    Iterates over every PENDING row in items_to_clone.
--
--    p_execute_mode = TRUE  → clone directly (live writes)
--    p_execute_mode = FALSE → accumulate SQL in clone_script_output
-- ============================================================

CREATE OR REPLACE PROCEDURE clone_items_batch(
    p_execute_mode BOOLEAN DEFAULT TRUE
)
LANGUAGE plpgsql
AS $$
DECLARE
v_rec           RECORD;
    v_new_uuid      UUID;
    v_sql_line      TEXT;
    v_processed     INT := 0;
    v_errors        INT := 0;
BEGIN
    IF NOT p_execute_mode THEN
        -- Prepare output table
        TRUNCATE TABLE clone_script_output;
INSERT INTO clone_script_output (sql_line) VALUES ('BEGIN;');
INSERT INTO clone_script_output (sql_line) VALUES ('');
END IF;

FOR v_rec IN
SELECT id,
       source_item_uuid,
       target_collection_uuid,
       new_handle,
       clone_relationships
FROM   items_to_clone
WHERE  status = 'PENDING'
ORDER  BY id
    LOOP
BEGIN
            IF p_execute_mode THEN
                -- ---- LIVE EXECUTION ----
                v_new_uuid := clone_item_direct(
                    v_rec.source_item_uuid,
                    v_rec.target_collection_uuid,
                    v_rec.new_handle,
                    COALESCE(v_rec.clone_relationships, TRUE)
                );

UPDATE items_to_clone
SET    status        = 'DONE',
       new_item_uuid = v_new_uuid,
       cloned_at     = NOW()
WHERE  id = v_rec.id;

INSERT INTO clone_log
(source_item_uuid, new_item_uuid, status, message)
VALUES
    (v_rec.source_item_uuid, v_new_uuid, 'DONE', 'Cloned successfully');

RAISE NOTICE '[clone_items_batch] % → % (DONE)', v_rec.source_item_uuid, v_new_uuid;
                v_processed := v_processed + 1;

ELSE
                -- ---- SQL GENERATION ----
                FOR v_sql_line IN
SELECT s
FROM   generate_clone_sql(
               v_rec.source_item_uuid,
               v_rec.target_collection_uuid,
               v_rec.new_handle,
               COALESCE(v_rec.clone_relationships, TRUE)
       ) s
    LOOP
INSERT INTO clone_script_output (sql_line) VALUES (v_sql_line);
END LOOP;

                v_processed := v_processed + 1;
                RAISE NOTICE '[clone_items_batch] Generated SQL for item %', v_rec.source_item_uuid;
END IF;

EXCEPTION WHEN OTHERS THEN
            -- Record the error but continue with the next item
UPDATE items_to_clone
SET    status        = 'ERROR',
       error_message = SQLERRM
WHERE  id = v_rec.id;

INSERT INTO clone_log
(source_item_uuid, new_item_uuid, status, message)
VALUES
    (v_rec.source_item_uuid, NULL, 'ERROR', SQLERRM);

RAISE WARNING '[clone_items_batch] Error cloning %: %', v_rec.source_item_uuid, SQLERRM;
            v_errors := v_errors + 1;
END;
END LOOP;

    IF NOT p_execute_mode THEN
        INSERT INTO clone_script_output (sql_line) VALUES ('');
INSERT INTO clone_script_output (sql_line) VALUES ('COMMIT;');
RAISE NOTICE '[clone_items_batch] Script written to clone_script_output (% items, % errors).', v_processed, v_errors;
        RAISE NOTICE 'Export with: \copy (SELECT sql_line FROM clone_script_output ORDER BY line_num) TO ''/path/to/clone_output.sql''';
ELSE
        RAISE NOTICE '[clone_items_batch] Complete: % cloned, % errors.', v_processed, v_errors;
END IF;
END;
$$;

COMMENT ON PROCEDURE clone_items_batch IS
  'Process all PENDING rows in items_to_clone. '
  'Pass TRUE to execute live, FALSE to generate SQL into clone_script_output.';


-- ============================================================
-- 7. CONVENIENCE PROCEDURE  –  generate_clone_script_to_table()
--    Shorthand for clone_items_batch(FALSE).
-- ============================================================

CREATE OR REPLACE PROCEDURE generate_clone_script_to_table()
LANGUAGE plpgsql
AS $$
BEGIN
CALL clone_items_batch(FALSE);
END;
$$;

COMMENT ON PROCEDURE generate_clone_script_to_table IS
  'Shorthand: generates an SQL script for all PENDING items into clone_script_output. '
  'Equivalent to CALL clone_items_batch(FALSE).';


-- ============================================================
-- 8. UTILITY VIEWS
-- ============================================================

-- Quick summary of what has been cloned
CREATE OR REPLACE VIEW v_clone_status AS
SELECT
    itc.id,
    itc.source_item_uuid,
    itc.new_item_uuid,
    itc.status,
    itc.target_collection_uuid,
    itc.new_handle,
    itc.clone_relationships,
    itc.cloned_at,
    itc.error_message,
    -- Peek at the source item's dc.title for convenience
    (SELECT mv.text_value
     FROM   metadatavalue mv
                JOIN   metadatafieldregistry mf ON mf.metadata_field_id = mv.metadata_field_id
                JOIN   metadataschemaregistry ms ON ms.metadata_schema_id = mf.metadata_schema_id
     WHERE  mv.dspace_object_id = itc.source_item_uuid
       AND  ms.short_id   = 'dc'
       AND  mf.element    = 'title'
       AND  mf.qualifier  IS NULL
     ORDER  BY mv.place
        LIMIT  1) AS source_title
FROM items_to_clone itc;

COMMENT ON VIEW v_clone_status IS 'Summary view of items_to_clone with source title for convenience.';


-- ============================================================
-- END OF SCRIPT
-- ============================================================
--
-- NOTES FOR DSpace OPERATORS
-- --------------------------
-- After cloning items in DIRECT mode, remember to:
--   1. Reindex Solr:
--        [dspace]/bin/dspace index-discovery -f
--   2. Update any OAI-PMH caches if OAI is enabled.
--   3. If the new items should NOT yet be discoverable, set
--      discoverable = FALSE in items_to_clone (inherited from source)
--      or update the cloned rows directly after cloning.
--   4. Handles: DSpace discovery and the REST API rely on handles
--      for item look-up.  Either supply a new_handle per item, or
--      register handles through the normal DSpace handle server
--      after importing.
--
-- RELATIONSHIP CLONING STRATEGY
-- ------------------------------
-- When clone_relationships = TRUE (the default), every relationship
-- in which the source item participates is duplicated for the clone.
-- Examples:
--   • Publication ←isAuthorOf→ Person
--     Clone of Publication ←isAuthorOf→ (same) Person
--   • If BOTH items in a relationship are being cloned in the same
--     batch, each clone will reference the *original* partner item
--     (not the other clone), because the batch processes items
--     independently.  If you need clone-to-clone relationships,
--     insert a second batch referencing the first batch's new UUIDs.
--
-- SEQUENCE SAFETY
-- ---------------
-- The script uses nextval(...) explicitly for:
--   metadatavalue_seq, resourcepolicy_seq,
--   relationship_id_seq, handle_id_seq
-- This works regardless of whether the column has a DEFAULT clause.
-- ============================================================

