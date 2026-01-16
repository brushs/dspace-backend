/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.publicationrequest.dao.PublicationRequestDAO;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the PublicationRequest object. This class is responsible for
 * all business logic calls for the PublicationRequest object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class PublicationRequestServiceImpl implements PublicationRequestService {

    /**
     * log4j logger
     */
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(PublicationRequestServiceImpl.class);

    @Autowired(required = true)
    protected PublicationRequestDAO publicationRequestDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected PublicationRequestServiceImpl() {
    }

    @Override
    public PublicationRequest create(Context context) throws SQLException, AuthorizeException {
        // Note: Creation is public, no authorization check needed
        // Just create the object without persisting yet - values will be set before saving
        PublicationRequest publicationRequest = new PublicationRequest();
        return publicationRequest;
    }

    @Override
    public PublicationRequest find(Context context, Integer id) throws SQLException {
        return publicationRequestDAO.findByID(context, id);
    }

    @Override
    public void update(Context context, PublicationRequest publicationRequest)
        throws SQLException, AuthorizeException {
        // Only administrators can update
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can update publication requests");
        }
        publicationRequestDAO.save(context, publicationRequest);
        log.info("Updated PublicationRequest with ID: " + publicationRequest.getId());
    }

    @Override
    public void updateWithoutAuthCheck(Context context, PublicationRequest publicationRequest)
        throws SQLException {
        // No authorization check - used for public creation only
        // Check if this is a new entity (no ID) or an existing one
        if (publicationRequest.getId() == null) {
            // New entity - use create
            publicationRequest = publicationRequestDAO.create(context, publicationRequest);
            log.info("Created PublicationRequest with ID: " + publicationRequest.getId());
        } else {
            // Existing entity - use save
            publicationRequestDAO.save(context, publicationRequest);
            log.info("Updated PublicationRequest with ID: " + publicationRequest.getId());
        }
    }

    @Override
    public void delete(Context context, PublicationRequest publicationRequest)
        throws SQLException, AuthorizeException {
        // Only administrators can delete
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can delete publication requests");
        }
        publicationRequestDAO.delete(context, publicationRequest);
        log.info("Deleted PublicationRequest with ID: " + publicationRequest.getId());
    }

    @Override
    public List<PublicationRequest> findByPublicationUUID(Context context, String publicationUUID)
        throws SQLException {
        return publicationRequestDAO.findByPublicationUUID(context, publicationUUID);
    }

    @Override
    public List<PublicationRequest> findByUserEmailAddress(Context context, String userEmailAddress)
        throws SQLException {
        return publicationRequestDAO.findByUserEmailAddress(context, userEmailAddress);
    }

    @Override
    public List<PublicationRequest> findAll(Context context, int offset, int limit) throws SQLException {
        return publicationRequestDAO.findAll(context, offset, limit);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return publicationRequestDAO.countRows(context);
    }

    @Override
    public List<PublicationRequest> findByTitle(Context context, String title, int offset, int limit)
        throws SQLException {
        return publicationRequestDAO.findByTitle(context, title, offset, limit);
    }

    @Override
    public int countByTitle(Context context, String title) throws SQLException {
        return publicationRequestDAO.countByTitle(context, title);
    }

    @Override
    public List<PublicationRequest> findByTranslationRequestId(Context context, Integer translationRequestId,
                                                                 int offset, int limit) throws SQLException {
        return publicationRequestDAO.findByTranslationRequestId(context, translationRequestId, offset, limit);
    }

    @Override
    public int countByTranslationRequestId(Context context, Integer translationRequestId) throws SQLException {
        return publicationRequestDAO.countByTranslationRequestId(context, translationRequestId);
    }
}

