--------------------------------------------------------------------------------
-- Create translation2publication table to link translation requests to
-- publication requests
--------------------------------------------------------------------------------

CREATE TABLE translation2publication (
                                         translationrequest_id INTEGER NOT NULL,
                                         publicationrequest_id INTEGER NOT NULL,
                                         PRIMARY KEY (translationrequest_id, publicationrequest_id)
);

-- Add foreign key constraints if the parent tables exist
-- Uncomment these if you want to enforce referential integrity
ALTER TABLE translation2publication
     ADD CONSTRAINT fk_translation2publication_translation
     FOREIGN KEY (translationrequest_id)
     REFERENCES translationrequest(translationrequest_id)
     ON DELETE CASCADE;

ALTER TABLE translation2publication
     ADD CONSTRAINT fk_translation2publication_publication
     FOREIGN KEY (publicationrequest_id)
     REFERENCES publicationrequest(publicationrequest_id)
     ON DELETE CASCADE;

-- Create indexes for faster lookups
CREATE INDEX idx_translation2publication_translation
    ON translation2publication(translationrequest_id);

CREATE INDEX idx_translation2publication_publication
    ON translation2publication(publicationrequest_id);
