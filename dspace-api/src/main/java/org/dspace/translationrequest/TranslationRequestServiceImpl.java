/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translationrequest;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.translationrequest.dao.TranslationRequestDAO;
import org.dspace.translationrequest.service.TranslationRequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the TranslationRequest object. This class is responsible for
 * all business logic calls for the TranslationRequest object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class TranslationRequestServiceImpl implements TranslationRequestService {

    /**
     * log4j logger
     */
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(TranslationRequestServiceImpl.class);

    @Autowired(required = true)
    protected TranslationRequestDAO translationRequestDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected TranslationRequestServiceImpl() {
    }

    @Override
    public TranslationRequest create(Context context) throws SQLException, AuthorizeException {
        // Note: Creation is public, no authorization check needed
        // Just create the object without persisting yet - values will be set before saving
        TranslationRequest translationRequest = new TranslationRequest();
        return translationRequest;
    }

    @Override
    public TranslationRequest find(Context context, Integer id) throws SQLException {
        return translationRequestDAO.findByID(context, id);
    }

    @Override
    public void update(Context context, TranslationRequest translationRequest)
        throws SQLException, AuthorizeException {
        // Only administrators can update
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can update translation requests");
        }
        translationRequestDAO.save(context, translationRequest);
        log.info("Updated TranslationRequest with ID: " + translationRequest.getId());
    }

    @Override
    public void updateWithoutAuthCheck(Context context, TranslationRequest translationRequest)
        throws SQLException {
        // No authorization check - used for public creation only
        // Check if this is a new entity (no ID) or an existing one
        if (translationRequest.getId() == null) {
            // New entity - use create
            translationRequest = translationRequestDAO.create(context, translationRequest);
            log.info("Created TranslationRequest with ID: " + translationRequest.getId());
        } else {
            // Existing entity - use save
            translationRequestDAO.save(context, translationRequest);
            log.info("Updated TranslationRequest with ID: " + translationRequest.getId());
        }
    }

    @Override
    public void delete(Context context, TranslationRequest translationRequest)
        throws SQLException, AuthorizeException {
        // Only administrators can delete
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can delete translation requests");
        }
        translationRequestDAO.delete(context, translationRequest);
        log.info("Deleted TranslationRequest with ID: " + translationRequest.getId());
    }

    @Override
    public List<TranslationRequest> findByPublicationUUID(Context context, String publicationUUID)
        throws SQLException {
        return translationRequestDAO.findByPublicationUUID(context, publicationUUID);
    }

    @Override
    public List<TranslationRequest> findAll(Context context, int offset, int limit) throws SQLException {
        return translationRequestDAO.findAll(context, offset, limit);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return translationRequestDAO.countRows(context);
    }
}

