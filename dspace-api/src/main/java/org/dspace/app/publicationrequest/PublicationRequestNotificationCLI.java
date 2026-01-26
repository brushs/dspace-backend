/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.publicationrequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.publicationrequest.PublicationRequest;
import org.dspace.publicationrequest.PublicationRequestStatus;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.gcnotify.GCNotifyService;
import org.dspace.utils.DSpace;

/**
 * Script to process PublicationRequests in "Pending Notification" status,
 * send email notifications via GC Notify, and update their status accordingly.
 *
 * @author [Your Name]
 */
public class PublicationRequestNotificationCLI extends DSpaceRunnable<PublicationRequestNotificationCLIScriptConfiguration> {

    private static final Logger log = LogManager.getLogger(PublicationRequestNotificationCLI.class);

    private PublicationRequestService publicationRequestService;
    private GCNotifyService gcNotifyService;
    private ItemService itemService;
    private HandleService handleService;
    private ConfigurationService configurationService;

    @Override
    public PublicationRequestNotificationCLIScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("publication-request-notification", PublicationRequestNotificationCLIScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        publicationRequestService = new DSpace().getServiceManager()
            .getServiceByName("publicationRequestService", PublicationRequestService.class);
        gcNotifyService = new DSpace().getServiceManager()
            .getServiceByName("gcNotifyService", GCNotifyService.class);
        itemService = new DSpace().getServiceManager()
            .getServiceByName("itemService", ItemService.class);
        handleService = new DSpace().getServiceManager()
            .getServiceByName("handleService", HandleService.class);
        configurationService = new DSpace().getConfigurationService();
    }

    @Override
    public void internalRun() throws Exception {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            processNotifications(context);
            context.complete();
        } catch (Exception e) {
            log.error("Error processing publication request notifications", e);
            context.abort();
            throw e;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Process all PublicationRequests in "Pending Notification" and "No Translation Needed" statuses
     */
    private void processNotifications(Context context) throws SQLException {
        log.info("Starting publication request notification processing...");

        // Get the configurable delay in hours for "No Translation Needed" requests
        int noTranslationNeededDelayHours = configurationService.getIntProperty(
            "publicationrequest.notification.notranslation.delay.hours", 0);

        log.info("No Translation Needed delay configured: {} hours", noTranslationNeededDelayHours);

        // Find all PublicationRequests with "Pending Notification" status (ID=3)
        List<PublicationRequest> pendingRequests = publicationRequestService.findByStatus(
            context,
            PublicationRequestStatus.PENDING_NOTIFICATION.getId(),
            0,
            -1  // Get all
        );

        // Find all PublicationRequests with "No Translation Needed" status (ID=8)
        List<PublicationRequest> noTranslationNeededRequests = publicationRequestService.findByStatus(
            context,
            PublicationRequestStatus.NO_TRANSLATION_NEEDED.getId(),
            0,
            -1  // Get all
        );

        int totalPendingCount = (pendingRequests != null ? pendingRequests.size() : 0);
        int totalNoTranslationCount = (noTranslationNeededRequests != null ? noTranslationNeededRequests.size() : 0);

        if (totalPendingCount == 0 && totalNoTranslationCount == 0) {
            log.info("No publication requests found in 'Pending Notification' or 'No Translation Needed' status.");
            return;
        }

        log.info("Found {} 'Pending Notification' and {} 'No Translation Needed' publication request(s). Processing...",
            totalPendingCount, totalNoTranslationCount);

        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        // Process Pending Notification requests (no delay check needed)
        if (pendingRequests != null && !pendingRequests.isEmpty()) {
            for (PublicationRequest request : pendingRequests) {
                try {
                    processRequest(context, request);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to process PublicationRequest ID: " + request.getId(), e);
                    updateStatusToError(context, request, e);
                }
            }
        }

        // Process No Translation Needed requests (with delay check)
        if (noTranslationNeededRequests != null && !noTranslationNeededRequests.isEmpty()) {
            for (PublicationRequest request : noTranslationNeededRequests) {
                try {
                    // Check if request is old enough to be processed
                    if (!isRequestOldEnough(request, noTranslationNeededDelayHours)) {
                        skippedCount++;
                        log.info("Skipping PublicationRequest ID: {} - not old enough (created less than {} hours ago)",
                            request.getId(), noTranslationNeededDelayHours);
                        continue;
                    }

                    processRequest(context, request);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to process PublicationRequest ID: " + request.getId(), e);
                    updateStatusToError(context, request, e);
                }
            }
        }

        log.info("Publication request notification processing complete. Success: {}, Errors: {}, Skipped: {}",
            successCount, errorCount, skippedCount);
    }

    /**
     * Check if a PublicationRequest is old enough to be processed based on configured delay
     *
     * @param request The PublicationRequest to check
     * @param delayHours The minimum age in hours before processing
     * @return true if the request is old enough to be processed
     */
    private boolean isRequestOldEnough(PublicationRequest request, int delayHours) {
        // If no delay configured, process immediately
        if (delayHours <= 0) {
            return true;
        }

        // Get created date from the request
        // Note: PublicationRequest doesn't have a created_date field currently
        // We'll need to add this field or use another approach
        // For now, we'll check if the field exists via a custom method

        try {
            // Calculate the threshold time
            long delayMillis = delayHours * 60L * 60L * 1000L;
            long thresholdTime = System.currentTimeMillis() - delayMillis;

            // Note: This requires a created_date field on PublicationRequest
            // If it doesn't exist, this will need to be added to the entity
            java.util.Date createdDate = getCreatedDate(request);

            if (createdDate == null) {
                log.warn("PublicationRequest ID: {} has no created date, processing anyway", request.getId());
                return true;
            }

            long createdTime = createdDate.getTime();
            boolean isOldEnough = createdTime <= thresholdTime;

            if (isOldEnough) {
                log.debug("PublicationRequest ID: {} created at {} is old enough to process (threshold: {})",
                    request.getId(), createdDate, new java.util.Date(thresholdTime));
            } else {
                log.debug("PublicationRequest ID: {} created at {} is too recent (threshold: {})",
                    request.getId(), createdDate, new java.util.Date(thresholdTime));
            }

            return isOldEnough;

        } catch (Exception e) {
            log.error("Error checking age of PublicationRequest ID: " + request.getId() + ", processing anyway", e);
            return true;
        }
    }

    /**
     * Get the created date of a PublicationRequest
     */
    private java.util.Date getCreatedDate(PublicationRequest request) {
        return request.getCreatedDate();
    }
}
    /**
     * Process a single PublicationRequest
     */
    private void processRequest(Context context, PublicationRequest request) throws Exception {
        log.info("Processing PublicationRequest ID: {} for user: {}",
            request.getId(), request.getUserEmailAddress());

        // Get the item handle
        String itemHandle = getItemHandle(context, request.getPublicationUUID());
        if (itemHandle == null) {
            throw new Exception("Could not retrieve handle for item UUID: " + request.getPublicationUUID());
        }

        log.info("Item handle: {} for PublicationRequest ID: {}", itemHandle, request.getId());

        // Get the base URL for constructing the link
        String baseUrl = configurationService.getProperty("dspace.ui.url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = configurationService.getProperty("dspace.server.url");
        }
        String itemUrl = baseUrl + "/handle/" + itemHandle;

        // Send email via GC Notify
        log.info("Sending notification email to: {} for PublicationRequest ID: {}",
            request.getUserEmailAddress(), request.getId());

        // Get template ID from configuration
        String templateId = configurationService.getProperty("gc.notify.template.publication.notification");
        if (templateId == null || templateId.isEmpty()) {
            throw new Exception("GC Notify template ID not configured: gc.notify.template.publication.notification");
        }

        // Send email with the item URL as a parameter
        gcNotifyService.sendEmail(
            request.getUserEmailAddress(),
            templateId,
            java.util.Collections.singletonMap("link", itemUrl)
        );

        log.info("Successfully sent notification email for PublicationRequest ID: {}", request.getId());

        // Update status to Completed
        request.setStatus(PublicationRequestStatus.COMPLETED.getId());
        publicationRequestService.updateWithoutAuthCheck(context, request);

        log.info("Updated PublicationRequest ID: {} status to 'Completed'", request.getId());
    }

    /**
     * Get the handle for an item
     */
    private String getItemHandle(Context context, String publicationUUID) {
        try {
            UUID itemUUID = UUID.fromString(publicationUUID);
            Item item = itemService.find(context, itemUUID);
            if (item != null) {
                String handle = handleService.findHandle(context, item);
                return handle;
            }
        } catch (Exception e) {
            log.error("Error retrieving handle for item UUID: " + publicationUUID, e);
        }
        return null;
    }

    /**
     * Update the request status to Error and log the full error details
     */
    private void updateStatusToError(Context context, PublicationRequest request, Exception e) {
        try {
            // Get full stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Log the full error
            log.error("ERROR processing PublicationRequest ID: {}. Setting status to 'Error'. " +
                      "User email: {}. Publication UUID: {}. Error message: {}. Stack trace: {}",
                request.getId(),
                request.getUserEmailAddress(),
                request.getPublicationUUID(),
                e.getMessage(),
                stackTrace);

            // Update status to Error (ID=7)
            request.setStatus(PublicationRequestStatus.ERROR.getId());
            publicationRequestService.updateWithoutAuthCheck(context, request);

            log.error("Updated PublicationRequest ID: {} status to 'Error'", request.getId());
        } catch (Exception updateException) {
            log.error("Failed to update PublicationRequest ID: {} to Error status",
                request.getId(), updateException);
        }
    }
}
