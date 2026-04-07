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
 * Script to process PublicationRequest notifications on-demand via REST API/UI.
 * This bypasses the Curator framework to avoid the 200K item iteration issue.
 *
 * This is functionally identical to PublicationRequestNotificationCLI but designed
 * to be invoked via REST API instead of command line.
 *
 * @author DSpace Community
 */
public class PublicationRequestNotificationScript extends DSpaceRunnable<PublicationRequestNotificationScriptConfiguration> {

    private static final Logger log = LogManager.getLogger(PublicationRequestNotificationScript.class);

    private PublicationRequestService publicationRequestService;
    private GCNotifyService gcNotifyService;
    private ItemService itemService;
    private HandleService handleService;
    private ConfigurationService configurationService;

    @Override
    public PublicationRequestNotificationScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("publication-request-notification-script",
                PublicationRequestNotificationScriptConfiguration.class);
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
            log.info("Starting publication request notification processing via script (not curation task)");

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
            handler.logInfo("Publication Request Notification Processing Complete:");
            handler.logInfo("  Found: " + result.getTotalFound() + " total requests");
            handler.logInfo("  Success: " + result.getSuccessCount());
            handler.logInfo("  Errors: " + result.getErrorCount());
            handler.logInfo("  Skipped: " + result.getSkippedCount());

            context.complete();
        } catch (Exception e) {
            log.error("Error processing publication request notifications", e);
            handler.handleException(e);
            context.abort();
            throw e;
        } finally {
            context.restoreAuthSystemState();
        }
    }
}
