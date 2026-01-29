/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.publicationrequest.PublicationRequestNotificationProcessor;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.handle.service.HandleService;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.gcnotify.GCNotifyService;
import org.dspace.utils.DSpace;

/**
 * Curation task to process PublicationRequests in "Pending Notification" and "No Translation Needed" statuses,
 * send email notifications via GC Notify, and update their status accordingly.
 *
 * This task is meant to be run at the site level and will process all pending requests.
 * It provides an on-demand alternative to the scheduled CLI job.
 *
 * Uses the shared PublicationRequestNotificationProcessor for business logic.
 *
 * @author [Your Name]
 */
public class PublicationRequestNotification extends AbstractCurationTask {

    private static final Logger log = LogManager.getLogger(PublicationRequestNotification.class);

    private PublicationRequestService publicationRequestService;
    private GCNotifyService gcNotifyService;
    private ItemService itemServiceLocal;
    private HandleService handleServiceLocal;
    private ConfigurationService configurationServiceLocal;
    private SiteService siteService;

    private int status = Curator.CURATE_SUCCESS;
    private final StringBuilder resultMessage = new StringBuilder();

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        // Use inherited services from AbstractCurationTask
        itemServiceLocal = this.itemService;
        handleServiceLocal = this.handleService;
        configurationServiceLocal = this.configurationService;

        // Get SiteService
        siteService = ContentServiceFactory.getInstance().getSiteService();

        // Initialize custom services using DSpace service manager
        DSpace dspace = new DSpace();

        try {
            // Try to get publicationRequestService
            java.util.List<PublicationRequestService> prServices =
                dspace.getServiceManager().getServicesByType(PublicationRequestService.class);

            if (prServices == null || prServices.isEmpty()) {
                throw new IOException("PublicationRequestService is not registered in Spring container. " +
                    "Ensure PublicationRequestServiceImpl is properly configured as a Spring bean.");
            }
            publicationRequestService = prServices.get(0);
            log.debug("Successfully initialized PublicationRequestService");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get PublicationRequestService", e);
            throw new IOException("Failed to initialize publicationRequestService: " + e.getMessage(), e);
        }

        try {
            // Try to get gcNotifyService
            java.util.List<GCNotifyService> gcServices =
                dspace.getServiceManager().getServicesByType(GCNotifyService.class);

            if (gcServices == null || gcServices.isEmpty()) {
                throw new IOException("GCNotifyService is not registered in Spring container. " +
                    "Ensure GCNotifyService is properly configured as a Spring bean with @Service or @Component annotation.");
            }
            gcNotifyService = gcServices.get(0);
            log.debug("Successfully initialized GCNotifyService");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get GCNotifyService", e);
            throw new IOException("Failed to initialize gcNotifyService: " + e.getMessage(), e);
        }

        // Validate all services were initialized
        if (publicationRequestService == null) {
            throw new IOException("publicationRequestService is null after initialization");
        }
        if (gcNotifyService == null) {
            throw new IOException("gcNotifyService is null after initialization");
        }
        if (itemServiceLocal == null) {
            throw new IOException("itemService is null after initialization");
        }
        if (handleServiceLocal == null) {
            throw new IOException("handleService is null after initialization");
        }
        if (configurationServiceLocal == null) {
            throw new IOException("configurationService is null after initialization");
        }

        log.info("PublicationRequestNotification task initialized successfully");
    }

    /**
     * Override perform method to ALWAYS execute on Site object, regardless of what target is specified.
     * This ensures the task runs exactly once and processes all pending requests repository-wide.
     *
     * @param ctx DSpace context
     * @param id  The target identifier (ignored - always uses Site)
     * @return status code
     * @throws IOException if error occurs
     */
    @Override
    public int perform(Context ctx, String id) throws IOException {
        try {
            // ALWAYS get the Site object, regardless of what ID was passed
            Site site = siteService.findSite(ctx);

            if (site == null) {
                log.error("Unable to find Site object");
                status = Curator.CURATE_FAIL;
                resultMessage.append("ERROR: Unable to find Site object\n");
                setResult(resultMessage.toString());
                report(resultMessage.toString());
                return status;
            }

            // Log what target was requested vs what we're actually using
            if (id != null && !id.equals("site") && !id.equals("0")) {
                log.info("PublicationRequestNotification task was called with target '{}' but will execute on Site instead (this task only runs at repository level)", id);
            }

            // Call the main perform method with Site object
            return perform(site);

        } catch (SQLException e) {
            log.error("Error retrieving Site object", e);
            throw new IOException("Failed to get Site object: " + e.getMessage(), e);
        }
    }

    /**
     * Perform the curation task
     * This task should ONLY be run on the Site object to process all pending notifications.
     * It will not process individual items, collections, or communities.
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        // CRITICAL: This task ONLY works on Site objects
        // Skip all non-Site objects immediately without any logging
        if (!(dso instanceof Site)) {
            // Don't set result, don't log - just return skip status
            // This prevents the Curator from logging anything for these objects
            return Curator.CURATE_SKIP;
        }

        log.info("PublicationRequestNotification task executing");

        Context context;
        // ...existing code...
        try {
            context = Curator.curationContext();
        } catch (SQLException e) {
            status = Curator.CURATE_FAIL;
            log.error("Failed to get curation context", e);
            resultMessage.append("ERROR: Failed to get curation context: ").append(e.getMessage()).append("\n");
            setResult(resultMessage.toString());
            report(resultMessage.toString());
            return status;
        }

        try {
            // Validate all services before creating processor
            if (publicationRequestService == null || gcNotifyService == null ||
                itemServiceLocal == null || handleServiceLocal == null ||
                configurationServiceLocal == null) {
                throw new IOException("One or more required services are not initialized. " +
                    "publicationRequestService: " + (publicationRequestService != null) +
                    ", gcNotifyService: " + (gcNotifyService != null) +
                    ", itemService: " + (itemServiceLocal != null) +
                    ", handleService: " + (handleServiceLocal != null) +
                    ", configurationService: " + (configurationServiceLocal != null));
            }

            // Create the processor with required services
            PublicationRequestNotificationProcessor processor = new PublicationRequestNotificationProcessor(
                publicationRequestService,
                gcNotifyService,
                itemServiceLocal,
                handleServiceLocal,
                configurationServiceLocal
            );

            // Get the configured delay
            int delayHours = configurationServiceLocal.getIntProperty(
                "publicationrequest.notification.notranslation.delay.hours", 0);

            resultMessage.append("Starting publication request notification processing...\n");
            resultMessage.append("No Translation Needed delay: ").append(delayHours).append(" hours\n");

            log.info("Starting publication request notification processing via curation task");

            // Process notifications using the shared processor
            PublicationRequestNotificationProcessor.ProcessingResult result = processor.processNotifications(context);

            // Build result message
            if (result.getTotalFound() == 0) {
                resultMessage.append("No publication requests found in 'Pending Notification' or 'No Translation Needed' status.\n");
            } else {
                resultMessage.append("Found ").append(result.getTotalPendingCount()).append(" 'Pending Notification' and ")
                    .append(result.getTotalNoTranslationCount()).append(" 'No Translation Needed' request(s)\n");
                resultMessage.append("\nProcessing complete:\n");
                resultMessage.append("  Success: ").append(result.getSuccessCount()).append("\n");
                resultMessage.append("  Errors: ").append(result.getErrorCount()).append("\n");
                resultMessage.append("  Skipped: ").append(result.getSkippedCount()).append("\n");
            }

            // Set status based on results
            if (result.getErrorCount() > 0) {
                status = Curator.CURATE_FAIL;
            } else if (result.getSuccessCount() > 0) {
                status = Curator.CURATE_SUCCESS;
            } else {
                status = Curator.CURATE_SUCCESS; // No errors, even if nothing processed
            }

            setResult(resultMessage.toString());
            report(resultMessage.toString());

            log.info("PublicationRequestNotification task completed successfully");

        } catch (Exception e) {
            status = Curator.CURATE_FAIL;
            String errorMsg = "ERROR: Failed to process publication request notifications: " +
                (e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            log.error(errorMsg, e);

            // Add full stack trace to result message for debugging
            resultMessage.append(errorMsg).append("\n");
            resultMessage.append("Exception type: ").append(e.getClass().getName()).append("\n");
            if (e.getCause() != null) {
                resultMessage.append("Cause: ").append(e.getCause().getMessage()).append("\n");
            }

            setResult(resultMessage.toString());
            report(resultMessage.toString());
        }

        return status;
    }
}
