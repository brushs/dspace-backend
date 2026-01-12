-- ========================================
-- Create sequence for publication request ID
-- ========================================
CREATE SEQUENCE IF NOT EXISTS publicationrequest_seq START WITH 1 INCREMENT BY 1;

-- ========================================
-- Create publicationrequest table
-- ========================================
CREATE TABLE IF NOT EXISTS publicationrequest
(
    publicationrequest_id INTEGER PRIMARY KEY DEFAULT nextval('publicationrequest_seq'),
    publication_guid VARCHAR(255) NOT NULL,
    user_email_address VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    status INTEGER,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- ========================================
-- Create indexes for better query performance
-- ========================================
CREATE INDEX IF NOT EXISTS idx_publicationrequest_guid
    ON publicationrequest(publication_guid);

CREATE INDEX IF NOT EXISTS idx_publicationrequest_email
    ON publicationrequest(user_email_address);

CREATE INDEX IF NOT EXISTS idx_publicationrequest_created
    ON publicationrequest(created_date);

-- ========================================
-- Comments for documentation
-- ========================================
COMMENT ON TABLE publicationrequest IS 'Stores publication requests from users';
COMMENT ON COLUMN publicationrequest.publicationrequest_id IS 'Primary key';
COMMENT ON COLUMN publicationrequest.publication_guid IS 'GUID of the requested publication';
COMMENT ON COLUMN publicationrequest.user_email_address IS 'Email address of the requester';
COMMENT ON COLUMN publicationrequest.language IS 'Language preference for the request';
COMMENT ON COLUMN publicationrequest.status IS 'Status of the request (integer code)';
COMMENT ON COLUMN publicationrequest.created_date IS 'Timestamp when the request was created';

