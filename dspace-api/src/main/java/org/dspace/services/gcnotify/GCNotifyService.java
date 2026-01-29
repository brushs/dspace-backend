/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.gcnotify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for sending email notifications via GC Notify API.
 *
 * GC Notify is the Government of Canada's notification service.
 * API Documentation: https://api.notification.canada.ca/v2/openapi-en
 *
 * Usage example:
 * <pre>
 * {@code
 * Map<String, String> personalisation = new HashMap<>();
 * personalisation.put("name", "John Doe");
 * personalisation.put("itemTitle", "Research Paper");
 *
 * String notificationId = gcNotifyService.sendEmail(
 *     "user@example.com",
 *     "template-uuid-here",
 *     personalisation
 * );
 * }
 * </pre>
 *
 * @author DSpace Community
 */
public class GCNotifyService {

    private static final Logger log = LogManager.getLogger(GCNotifyService.class);

    private static final int TIMEOUT_SECONDS = 30;

    @Autowired
    private GCNotifyConfiguration config;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Send an email using GC Notify service.
     *
     * @param emailAddress The recipient's email address
     * @param templateId The GC Notify template ID (UUID) to use
     * @param personalisation Map of template variables and their values
     * @return The notification ID from GC Notify, or null if failed
     */
    public String sendEmail(String emailAddress, String templateId,
                           Map<String, String> personalisation) {

        if (!config.isEnabled()) {
            log.warn("GC Notify service is disabled. Email not sent to: {}", emailAddress);
            return null;
        }

        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            log.error("Email address cannot be null or empty");
            return null;
        }

        if (templateId == null || templateId.trim().isEmpty()) {
            log.error("Template ID cannot be null or empty");
            return null;
        }

        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("GC Notify API key is not configured");
            return null;
        }

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT_SECONDS * 1000)
            .setSocketTimeout(TIMEOUT_SECONDS * 1000)
            .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            String apiUrl = config.getApiUrl() + "/v2/notifications/email";
            HttpPost request = new HttpPost(apiUrl);

            // Set headers according to GC Notify API spec
            request.setHeader(HttpHeaders.AUTHORIZATION, "ApiKey-v1 " + apiKey);
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // Build request body according to GC Notify API spec
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email_address", emailAddress);
            requestBody.put("template_id", templateId);

            if (personalisation != null && !personalisation.isEmpty()) {
                requestBody.put("personalisation", personalisation);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            log.debug("Sending email via GC Notify to: {}, template: {}", emailAddress, templateId);

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode == 201) {
                    // Success - parse notification ID from response
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    String notificationId = (String) responseMap.get("id");

                    log.info("Email sent successfully via GC Notify to: {}. Notification ID: {}",
                             emailAddress, notificationId);
                    return notificationId;
                } else {
                    log.error("Failed to send email via GC Notify. Status: {}, Response: {}",
                             statusCode, responseBody);
                    return null;
                }
            }

        } catch (IOException e) {
            log.error("Error sending email via GC Notify to: {}", emailAddress, e);
            return null;
        }
    }

    /**
     * Send an email using the default template configured in dspace.cfg.
     *
     * @param emailAddress The recipient's email address
     * @param personalisation Map of template variables and their values
     * @return The notification ID from GC Notify, or null if failed
     */
    public String sendEmail(String emailAddress, Map<String, String> personalisation) {
        String templateId = config.getDefaultTemplateId();

        if (templateId == null || templateId.trim().isEmpty()) {
            log.error("No default template ID configured for GC Notify");
            return null;
        }

        return sendEmail(emailAddress, templateId, personalisation);
    }

    /**
     * Send a simple email with no personalisation variables.
     *
     * @param emailAddress The recipient's email address
     * @param templateId The GC Notify template ID (UUID) to use
     * @return The notification ID from GC Notify, or null if failed
     */
    public String sendEmail(String emailAddress, String templateId) {
        return sendEmail(emailAddress, templateId, null);
    }

    /**
     * Send an email using a template specified by configuration key.
     *
     * @param emailAddress The recipient's email address
     * @param templateKey The configuration key for the template (e.g., "publication.request")
     * @param personalisation Map of template variables and their values
     * @return The notification ID from GC Notify, or null if failed
     */
    public String sendEmailByTemplateKey(String emailAddress, String templateKey,
                                        Map<String, String> personalisation) {
        String templateId = config.getTemplateId(templateKey);

        if (templateId == null || templateId.trim().isEmpty()) {
            log.error("No template ID configured for key: {}", templateKey);
            return null;
        }

        return sendEmail(emailAddress, templateId, personalisation);
    }

    /**
     * Check if the GC Notify service is properly configured and enabled.
     *
     * @return true if the service is ready to send emails
     */
    public boolean isConfigured() {
        return config.isEnabled()
            && config.getApiKey() != null
            && !config.getApiKey().trim().isEmpty();
    }
}
