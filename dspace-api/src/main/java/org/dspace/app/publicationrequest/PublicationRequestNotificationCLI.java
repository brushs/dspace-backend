/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.publicationrequest;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
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
        DSpace dspace = new DSpace();

        // Get services by type since they don't have explicit bean IDs
        publicationRequestService = dspace.getSingletonService(PublicationRequestService.class);
        gcNotifyService = dspace.getSingletonService(GCNotifyService.class);
        itemService = dspace.getSingletonService(ItemService.class);
        handleService = dspace.getSingletonService(HandleService.class);
        configurationService = dspace.getConfigurationService();

        // Validate that all required services were loaded
        if (publicationRequestService == null) {
            throw new ParseException("Failed to load PublicationRequestService");
        }
        if (gcNotifyService == null) {
            throw new ParseException("Failed to load GCNotifyService");
        }
        if (itemService == null) {
            throw new ParseException("Failed to load ItemService");
        }
        if (handleService == null) {
            throw new ParseException("Failed to load HandleService");
        }
        if (configurationService == null) {
            throw new ParseException("Failed to load ConfigurationService");
        }

        log.info("All required services loaded successfully");
    }

    @Override
    public void internalRun() throws Exception {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            // Create the processor with required services
            PublicationRequestNotificationProcessor processor = new PublicationRequestNotificationProcessor(
                publicationRequestService,
                gcNotifyService,
                itemService,
                handleService,
                configurationService
            );

            // Process notifications using the shared processor
            PublicationRequestNotificationProcessor.ProcessingResult result = processor.processNotifications(context);

            // Log summary
            log.info("Processing summary: Found {} total, Success: {}, Errors: {}, Skipped: {}",
                result.getTotalFound(), result.getSuccessCount(), result.getErrorCount(), result.getSkippedCount());

            context.complete();
        } catch (Exception e) {
            log.error("Error processing publication request notifications", e);
            context.abort();
            throw e;
        } finally {
            context.restoreAuthSystemState();
        }
    }
}
