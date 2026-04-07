/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.gcnotify;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Example service demonstrating how to use GC Notify for sending emails.
 * This class shows common email notification patterns.
 *
 * @author DSpace Community
 */
@Service
public class GCNotifyEmailHelper {

    private static final Logger log = LogManager.getLogger(GCNotifyEmailHelper.class);

    @Autowired
    private GCNotifyService gcNotifyService;

    /**
     * Send a publication request notification email.
     *
     * @param recipientEmail The recipient's email address
     * @param userName The user's name
     * @param itemTitle The title of the item/publication
     * @param requestId The request ID for reference
     * @return The notification ID, or null if failed
     */
    public String sendPublicationRequestNotification(String recipientEmail, String userName,
                                                     String itemTitle, Integer requestId) {
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("name", userName);
        personalisation.put("item_title", itemTitle);
        personalisation.put("request_id", String.valueOf(requestId));

        return gcNotifyService.sendEmailByTemplateKey(
            recipientEmail,
            "publication.request",
            personalisation
        );
    }

    /**
     * Send a translation request notification email.
     *
     * @param recipientEmail The recipient's email address
     * @param userName The user's name
     * @param itemTitle The title of the item
     * @param documentName The name of the document to translate
     * @param requestId The request ID for reference
     * @return The notification ID, or null if failed
     */
    public String sendTranslationRequestNotification(String recipientEmail, String userName,
                                                     String itemTitle, String documentName,
                                                     Integer requestId) {
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("name", userName);
        personalisation.put("item_title", itemTitle);
        personalisation.put("document_name", documentName);
        personalisation.put("request_id", String.valueOf(requestId));

        return gcNotifyService.sendEmailByTemplateKey(
            recipientEmail,
            "translation.request",
            personalisation
        );
    }

    /**
     * Send a simple notification email with custom variables.
     *
     * @param recipientEmail The recipient's email address
     * @param templateId The template ID to use
     * @param variables Map of template variables
     * @return The notification ID, or null if failed
     */
    public String sendCustomNotification(String recipientEmail, String templateId,
                                        Map<String, String> variables) {
        return gcNotifyService.sendEmail(recipientEmail, templateId, variables);
    }

    /**
     * Send a request status update email.
     *
     * @param recipientEmail The recipient's email address
     * @param userName The user's name
     * @param requestType The type of request (e.g., "publication", "translation")
     * @param requestId The request ID
     * @param status The new status
     * @return The notification ID, or null if failed
     */
    public String sendRequestStatusUpdate(String recipientEmail, String userName,
                                         String requestType, Integer requestId,
                                         String status) {
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("name", userName);
        personalisation.put("request_type", requestType);
        personalisation.put("request_id", String.valueOf(requestId));
        personalisation.put("status", status);

        return gcNotifyService.sendEmailByTemplateKey(
            recipientEmail,
            "request.status.update",
            personalisation
        );
    }

    /**
     * Check if GC Notify is properly configured.
     *
     * @return true if configured and ready to send emails
     */
    public boolean isGCNotifyAvailable() {
        return gcNotifyService.isConfigured();
    }
}
