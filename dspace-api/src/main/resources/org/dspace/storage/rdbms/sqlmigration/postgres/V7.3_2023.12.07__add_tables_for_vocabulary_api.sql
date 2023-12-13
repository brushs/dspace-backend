CREATE TABLE vocabulary
(
    id                        INTEGER PRIMARY KEY,
    name_en                   VARCHAR(64) NOT NULL,
    name_fr                   VARCHAR(64) NOT NULL,
    description_en            VARCHAR(512),
    description_fr            VARCHAR(512),
    active_ind                BOOL NOT NULL,
    created_by_user_id        VARCHAR(64) NOT NULL,
    created_date              DATE NOT NULL,
    last_updated_user_id      VARCHAR(64) NOT NULL,
    last_updated_date         DATE NOT NULL
);

CREATE TABLE term
(
    id                        INTEGER PRIMARY KEY,
    external_id               VARCHAR(64),
    name_en                   VARCHAR(64) NOT NULL,
    name_fr                   VARCHAR(64) NOT NULL,
    description_en            VARCHAR(512),
    description_fr            VARCHAR(512),
    active_ind                BOOL NOT NULL,
    vocabulary_id             INTEGER NOT NULL,
    parent_term_id            INTEGER,
    created_by_user_id        VARCHAR(64) NOT NULL,
    created_date              DATE NOT NULL,
    last_updated_user_id      VARCHAR(64) NOT NULL,
    last_updated_date         DATE NOT NULL
);
