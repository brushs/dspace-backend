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
import org.dspace.publicationrequest.PublicationRequest;
import org.dspace.publicationrequest.PublicationRequestStatus;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.dspace.translation2publication.Translation2Publication;
import org.dspace.translation2publication.service.Translation2PublicationService;
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

    @Autowired(required = true)
    protected PublicationRequestService publicationRequestService;

    @Autowired(required = true)
    protected Translation2PublicationService translation2PublicationService;

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

        // Get the old status before updating
        Integer oldStatus = null;
        if (translationRequest.getId() != null) {
            TranslationRequest existingRequest = translationRequestDAO.findByID(context,
                translationRequest.getId());
            if (existingRequest != null) {
                oldStatus = existingRequest.getStatus();
            }
        }

        // Save the translation request
        translationRequestDAO.save(context, translationRequest);
        log.info("Updated TranslationRequest with ID: " + translationRequest.getId());

        // Automated status update logic
        Integer newStatus = translationRequest.getStatus();

        // Check if status changed to a value that should trigger automation
        boolean shouldTriggerAutomation = false;
        Integer targetPublicationStatus = null;
        String statusName = null;

        // If status changed to "Translation In Progress" (ID=2)
        if (newStatus != null && newStatus.equals(TranslationRequestStatus.TRANSLATION_IN_PROGRESS.getId())
            && !newStatus.equals(oldStatus)) {
            shouldTriggerAutomation = true;
            targetPublicationStatus = PublicationRequestStatus.TRANSLATION_IN_PROGRESS.getId();
            statusName = "Translation In Progress";
        }
        // If status changed to "On Hold" (ID=5)
        else if (newStatus != null && newStatus.equals(TranslationRequestStatus.ON_HOLD.getId())
            && !newStatus.equals(oldStatus)) {
            shouldTriggerAutomation = true;
            targetPublicationStatus = PublicationRequestStatus.ON_HOLD.getId();
            statusName = "On Hold";
        }
        // If status changed to "Completed" (ID=3)
        else if (newStatus != null && newStatus.equals(TranslationRequestStatus.COMPLETED.getId())
            && !newStatus.equals(oldStatus)) {
            shouldTriggerAutomation = true;
            targetPublicationStatus = PublicationRequestStatus.PENDING_NOTIFICATION.getId();
            statusName = "Completed";
        }

        if (shouldTriggerAutomation && targetPublicationStatus != null) {
            log.info("TranslationRequest ID " + translationRequest.getId()
                + " status changed to '" + statusName + "'. Updating related PublicationRequests...");

            // Find all related PublicationRequests via Translation2Publication
            List<Translation2Publication> links = translation2PublicationService
                .findByTranslationRequestId(context, translationRequest.getId());

            for (Translation2Publication link : links) {
                try {
                    PublicationRequest publicationRequest = publicationRequestService
                        .find(context, link.getPublicationRequestId());

                    if (publicationRequest != null) {
                        // Update the publication request status
                        Integer pubOldStatus = publicationRequest.getStatus();
                        publicationRequest.setStatus(targetPublicationStatus);

                        // Save without auth check since this is an automated system action
                        publicationRequestService.updateWithoutAuthCheck(context, publicationRequest);

                        // Determine the target status name for logging
                        String targetStatusName = statusName;
                        if (targetPublicationStatus.equals(PublicationRequestStatus.PENDING_NOTIFICATION.getId())) {
                            targetStatusName = "Pending Notification";
                        }

                        log.info("Automatically updated PublicationRequest ID " + publicationRequest.getId()
                            + " status from " + pubOldStatus + " to '" + targetStatusName + "' (ID="
                            + targetPublicationStatus + ") due to TranslationRequest ID "
                            + translationRequest.getId() + " status change.");
                    }
                } catch (SQLException e) {
                    log.error("Error updating PublicationRequest linked to TranslationRequest ID "
                        + translationRequest.getId(), e);
                    // Continue processing other links even if one fails
                }
            }
        }
    }

    @Override
    public void updateWithoutAuthCheck(Context context, TranslationRequest translationRequest)
        throws SQLException {
        // No authorization check - used for public creation only
        // Check if this is a new entity (no ID) or an existing one
        boolean isNewEntity = (translationRequest.getId() == null);

        // Get the old status before updating (for existing entities)
        Integer oldStatus = null;
        if (!isNewEntity) {
            TranslationRequest existingRequest = translationRequestDAO.findByID(context,
                translationRequest.getId());
            if (existingRequest != null) {
                oldStatus = existingRequest.getStatus();
            }
        }

        if (isNewEntity) {
            // New entity - use create
            translationRequest = translationRequestDAO.create(context, translationRequest);
            log.info("Created TranslationRequest with ID: " + translationRequest.getId());
        } else {
            // Existing entity - use save
            translationRequestDAO.save(context, translationRequest);
            log.info("Updated TranslationRequest with ID: " + translationRequest.getId());
        }

        // Automated status update logic (only for updates, not new creations)
        if (!isNewEntity) {
            Integer newStatus = translationRequest.getStatus();

            // Check if status changed to a value that should trigger automation
            boolean shouldTriggerAutomation = false;
            Integer targetPublicationStatus = null;
            String statusName = null;

            // If status changed to "Translation In Progress" (ID=2)
            if (newStatus != null && newStatus.equals(TranslationRequestStatus.TRANSLATION_IN_PROGRESS.getId())
                && !newStatus.equals(oldStatus)) {
                shouldTriggerAutomation = true;
                targetPublicationStatus = PublicationRequestStatus.TRANSLATION_IN_PROGRESS.getId();
                statusName = "Translation In Progress";
            }
            // If status changed to "On Hold" (ID=5)
            else if (newStatus != null && newStatus.equals(TranslationRequestStatus.ON_HOLD.getId())
                && !newStatus.equals(oldStatus)) {
                shouldTriggerAutomation = true;
                targetPublicationStatus = PublicationRequestStatus.ON_HOLD.getId();
                statusName = "On Hold";
            }
            // If status changed to "Completed" (ID=3)
            else if (newStatus != null && newStatus.equals(TranslationRequestStatus.COMPLETED.getId())
                && !newStatus.equals(oldStatus)) {
                shouldTriggerAutomation = true;
                targetPublicationStatus = PublicationRequestStatus.PENDING_NOTIFICATION.getId();
                statusName = "Completed";
            }

            if (shouldTriggerAutomation && targetPublicationStatus != null) {
                log.info("TranslationRequest ID " + translationRequest.getId()
                    + " status changed to '" + statusName + "'. Updating related PublicationRequests...");

                try {
                    // Find all related PublicationRequests via Translation2Publication
                    List<Translation2Publication> links = translation2PublicationService
                        .findByTranslationRequestId(context, translationRequest.getId());

                    for (Translation2Publication link : links) {
                        try {
                            PublicationRequest publicationRequest = publicationRequestService
                                .find(context, link.getPublicationRequestId());

                            if (publicationRequest != null) {
                                // Update the publication request status
                                Integer pubOldStatus = publicationRequest.getStatus();
                                publicationRequest.setStatus(targetPublicationStatus);

                                // Save without auth check since this is an automated system action
                                publicationRequestService.updateWithoutAuthCheck(context, publicationRequest);

                                // Determine the target status name for logging
                                String targetStatusName = statusName;
                                if (targetPublicationStatus.equals(PublicationRequestStatus.PENDING_NOTIFICATION.getId())) {
                                    targetStatusName = "Pending Notification";
                                }

                                log.info("Automatically updated PublicationRequest ID " + publicationRequest.getId()
                                    + " status from " + pubOldStatus + " to '" + targetStatusName + "' (ID="
                                    + targetPublicationStatus + ") due to TranslationRequest ID "
                                    + translationRequest.getId() + " status change.");
                            }
                        } catch (SQLException e) {
                            log.error("Error updating PublicationRequest linked to TranslationRequest ID "
                                + translationRequest.getId(), e);
                            // Continue processing other links even if one fails
                        }
                    }
                } catch (SQLException e) {
                    log.error("Error finding linked PublicationRequests for TranslationRequest ID "
                        + translationRequest.getId(), e);
                }
            }
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
    public TranslationRequest findByBitstreamUUIDAndPublicationUUID(Context context, String bitstreamUUID,
                                                                      String publicationUUID) throws SQLException {
        return translationRequestDAO.findByBitstreamUUIDAndPublicationUUID(context, bitstreamUUID, publicationUUID);
    }

    @Override
    public List<TranslationRequest> findAll(Context context, int offset, int limit) throws SQLException {
        return translationRequestDAO.findAll(context, offset, limit);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return translationRequestDAO.countRows(context);
    }

    @Override
    public List<TranslationRequest> findByPublicationTitle(Context context, String title, int offset, int limit)
        throws SQLException {
        return translationRequestDAO.findByPublicationTitle(context, title, offset, limit);
    }

    @Override
    public int countByPublicationTitle(Context context, String title) throws SQLException {
        return translationRequestDAO.countByPublicationTitle(context, title);
    }

    @Override
    public List<TranslationRequest> findByUserEmail(Context context, String email, int offset, int limit)
        throws SQLException {
        return translationRequestDAO.findByUserEmail(context, email, offset, limit);
    }

    @Override
    public int countByUserEmail(Context context, String email) throws SQLException {
        return translationRequestDAO.countByUserEmail(context, email);
    }

    @Override
    public List<TranslationRequest> findByStatus(Context context, int status, int offset, int limit)
        throws SQLException {
        return translationRequestDAO.findByStatus(context, status, offset, limit);
    }

    @Override
    public int countByStatus(Context context, int status) throws SQLException {
        return translationRequestDAO.countByStatus(context, status);
    }
}

