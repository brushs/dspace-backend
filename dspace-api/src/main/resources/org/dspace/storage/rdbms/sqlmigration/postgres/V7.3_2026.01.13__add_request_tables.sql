-- ========================================
-- Create sequence for translation request ID
-- ========================================
CREATE SEQUENCE IF NOT EXISTS translationrequest_seq START WITH 1 INCREMENT BY 1;

-- ========================================
-- Create translationrequest table
-- ========================================
CREATE TABLE IF NOT EXISTS translationrequest
(
    translationrequest_id INTEGER PRIMARY KEY DEFAULT nextval('translationrequest_seq'),
    publication_uuid VARCHAR(255) NOT NULL,
    bitstream_uuid VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    status INTEGER,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_date TIMESTAMP,
    notes TEXT
    );

-- ========================================
-- Create indexes for better query performance
-- ========================================
CREATE INDEX IF NOT EXISTS idx_translationrequest_guid
    ON translationrequest(publication_uuid);

CREATE INDEX IF NOT EXISTS idx_translationrequest_guid
    ON translationrequest(bitstream_uuid);

CREATE INDEX IF NOT EXISTS idx_translationrequest_created
    ON translationrequest(created_date);

-- ========================================
-- Comments for documentation
-- ========================================
COMMENT ON TABLE translationrequest IS 'Stores translation requests from users';
COMMENT ON COLUMN translationrequest.translationrequest_id IS 'Primary key';
COMMENT ON COLUMN translationrequest.publication_uuid IS 'UUID of the requested publication';
COMMENT ON COLUMN translationrequest.publication_uuid IS 'UUID of the bitstream to be translated';
COMMENT ON COLUMN translationrequest.language IS 'Target language for translation';
COMMENT ON COLUMN translationrequest.status IS 'Status of the request (integer code)';
COMMENT ON COLUMN translationrequest.created_date IS 'Timestamp when the request was created';

