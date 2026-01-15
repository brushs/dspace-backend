/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translation2publication;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.translation2publication.dao.Translation2PublicationDAO;
import org.dspace.translation2publication.service.Translation2PublicationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Translation2Publication object. This class is responsible for
 * all business logic calls for the Translation2Publication object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class Translation2PublicationServiceImpl implements Translation2PublicationService {

    /**
     * log4j logger
     */
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(
        Translation2PublicationServiceImpl.class);

    @Autowired(required = true)
    protected Translation2PublicationDAO translation2PublicationDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected Translation2PublicationServiceImpl() {
    }

    @Override
    public Translation2Publication create(Context context) throws SQLException, AuthorizeException {
        // Allow anyone to create translation2publication links
        Translation2Publication translation2Publication = new Translation2Publication();
        return translation2Publication;
    }

    @Override
    public Translation2Publication find(Context context, Translation2PublicationId id) throws SQLException {
        return translation2PublicationDAO.findByID(context, id);
    }

    @Override
    public Translation2Publication find(Context context, Integer translationRequestId, Integer publicationRequestId)
        throws SQLException {
        Translation2PublicationId id = new Translation2PublicationId(translationRequestId, publicationRequestId);
        return translation2PublicationDAO.findByID(context, id);
    }

    @Override
    public void update(Context context, Translation2Publication translation2Publication)
        throws SQLException, AuthorizeException {
        // Allow anyone to update translation2publication links
        // Check if this is a new entity (no ID set) or an existing one
        if (translation2Publication.getTranslationRequestId() == null ||
            translation2Publication.getPublicationRequestId() == null) {
            throw new IllegalArgumentException("Both translationRequestId and publicationRequestId must be set");
        }

        // Check if it already exists
        Translation2PublicationId id = new Translation2PublicationId(
            translation2Publication.getTranslationRequestId(),
            translation2Publication.getPublicationRequestId()
        );
        Translation2Publication existing = translation2PublicationDAO.findByID(context, id);

        if (existing == null) {
            // New entity - use create
            translation2Publication = translation2PublicationDAO.create(context, translation2Publication);
            log.info("Created Translation2Publication with translationRequestId: " +
                     translation2Publication.getTranslationRequestId() +
                     ", publicationRequestId: " + translation2Publication.getPublicationRequestId());
        } else {
            // Existing entity - use save (though for this entity there's nothing to update)
            translation2PublicationDAO.save(context, translation2Publication);
            log.info("Updated Translation2Publication with translationRequestId: " +
                     translation2Publication.getTranslationRequestId() +
                     ", publicationRequestId: " + translation2Publication.getPublicationRequestId());
        }
    }

    @Override
    public void delete(Context context, Translation2Publication translation2Publication)
        throws SQLException, AuthorizeException {
        // Only administrators can delete
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can delete translation2publication links");
        }
        translation2PublicationDAO.delete(context, translation2Publication);
        log.info("Deleted Translation2Publication with translationRequestId: " +
                 translation2Publication.getTranslationRequestId() +
                 ", publicationRequestId: " + translation2Publication.getPublicationRequestId());
    }

    @Override
    public List<Translation2Publication> findByTranslationRequestId(Context context, Integer translationRequestId)
        throws SQLException {
        return translation2PublicationDAO.findByTranslationRequestId(context, translationRequestId);
    }

    @Override
    public List<Translation2Publication> findByPublicationRequestId(Context context, Integer publicationRequestId)
        throws SQLException {
        return translation2PublicationDAO.findByPublicationRequestId(context, publicationRequestId);
    }

    @Override
    public List<Translation2Publication> findAll(Context context, int offset, int limit) throws SQLException {
        return translation2PublicationDAO.findAll(context, offset, limit);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return translation2PublicationDAO.countRows(context);
    }
}

