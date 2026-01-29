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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.publicationrequest.PublicationRequest;
import org.dspace.publicationrequest.PublicationRequestStatus;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.gcnotify.GCNotifyService;

/**
 * Helper class containing the shared business logic for processing PublicationRequest notifications.
 * This logic is used by both the CLI job and the curation task.
 *
 * @author [Your Name]
 */
public class PublicationRequestNotificationProcessor {

    private static final Logger log = LogManager.getLogger(PublicationRequestNotificationProcessor.class);

    private final PublicationRequestService publicationRequestService;
    private final GCNotifyService gcNotifyService;
    private final ItemService itemService;
    private final HandleService handleService;
    private final ConfigurationService configurationService;

    /**
     * Constructor
     */
    public PublicationRequestNotificationProcessor(
            PublicationRequestService publicationRequestService,
            GCNotifyService gcNotifyService,
            ItemService itemService,
            HandleService handleService,
            ConfigurationService configurationService) {
        this.publicationRequestService = publicationRequestService;
        this.gcNotifyService = gcNotifyService;
        this.itemService = itemService;
        this.handleService = handleService;
        this.configurationService = configurationService;
    }

    /**
     * Result object containing processing statistics
     */
    public static class ProcessingResult {
        private int successCount;
        private int errorCount;
        private int skippedCount;
        private int totalPendingCount;
        private int totalNoTranslationCount;

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void setSkippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
        }

        public int getTotalPendingCount() {
            return totalPendingCount;
        }

        public void setTotalPendingCount(int totalPendingCount) {
            this.totalPendingCount = totalPendingCount;
        }

        public int getTotalNoTranslationCount() {
            return totalNoTranslationCount;
        }

        public void setTotalNoTranslationCount(int totalNoTranslationCount) {
            this.totalNoTranslationCount = totalNoTranslationCount;
        }

        public int getTotalFound() {
            return totalPendingCount + totalNoTranslationCount;
        }
    }

    /**
     * Process all PublicationRequests in "Pending Notification" and "No Translation Needed" statuses
     *
     * @param context The DSpace context
     * @return Processing result with statistics
     * @throws SQLException if database error occurs
     */
    public ProcessingResult processNotifications(Context context) throws SQLException {
        log.info("Starting publication request notification processing...");

        ProcessingResult result = new ProcessingResult();

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

        result.setTotalPendingCount(pendingRequests != null ? pendingRequests.size() : 0);
        result.setTotalNoTranslationCount(noTranslationNeededRequests != null ? noTranslationNeededRequests.size() : 0);

        if (result.getTotalFound() == 0) {
            log.info("No publication requests found in 'Pending Notification' or 'No Translation Needed' status.");
            return result;
        }

        log.info("Found {} 'Pending Notification' and {} 'No Translation Needed' publication request(s). Processing...",
            result.getTotalPendingCount(), result.getTotalNoTranslationCount());

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

        result.setSuccessCount(successCount);
        result.setErrorCount(errorCount);
        result.setSkippedCount(skippedCount);

        log.info("Publication request notification processing complete. Success: {}, Errors: {}, Skipped: {}",
            successCount, errorCount, skippedCount);

        return result;
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

        try {
            // Calculate the threshold time
            long delayMillis = delayHours * 60L * 60L * 1000L;
            long thresholdTime = System.currentTimeMillis() - delayMillis;

            java.util.Date createdDate = request.getCreatedDate();

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

        // Send email with the item URL and name as parameters
        // Create personalisation map with required template variables
        java.util.Map<String, String> personalisation = new java.util.HashMap<>();
        personalisation.put("link", itemUrl);
        personalisation.put("name", "User"); // Dummy name as user name is not stored in PublicationRequest

        gcNotifyService.sendEmail(
            request.getUserEmailAddress(),
            templateId,
            personalisation
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
                return handleService.findHandle(context, item);
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
