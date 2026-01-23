/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.gcnotify;

import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration provider for GC Notify service settings.
 *
 * @author DSpace Community
 */
@Component
public class GCNotifyConfiguration {

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Get the GC Notify API key from configuration.
     *
     * @return The API key
     */
    public String getApiKey() {
        return configurationService.getProperty("gcnotify.api.key");
    }

    /**
     * Get the GC Notify API base URL.
     *
     * @return The API URL, defaults to https://api.notification.canada.ca
     */
    public String getApiUrl() {
        return configurationService.getProperty("gcnotify.api.url",
            "https://api.notification.canada.ca");
    }

    /**
     * Get the default template ID for emails.
     *
     * @return The default template ID
     */
    public String getDefaultTemplateId() {
        return configurationService.getProperty("gcnotify.template.id");
    }

    /**
     * Check if GC Notify service is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return configurationService.getBooleanProperty("gcnotify.enabled", false);
    }

    /**
     * Get a specific template ID by name/key.
     *
     * @param templateKey The configuration key for the template
     * @return The template ID, or null if not found
     */
    public String getTemplateId(String templateKey) {
        return configurationService.getProperty("gcnotify.template." + templateKey);
    }
}
