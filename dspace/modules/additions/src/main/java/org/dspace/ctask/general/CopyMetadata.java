/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Term;
import org.dspace.content.factory.VocabularyServiceFactory;
import org.dspace.content.service.VocabularyService;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CopyMetadata extends AbstractCurationTask {
    // Curation task status
    private int status = Curator.CURATE_SUCCESS;
    // The distributed boolean has a default value of 'false' for safest operation
    private boolean distributed = false;
    // Prefix for configuration module
    private static final String PLUGIN_PREFIX = "copy-metadata";
    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CopyMetadata.class);

    protected VocabularyService vocabularyService;

    /**
     * Initialise the curation task and read configuration, instantiate the DOI provider
     */
    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        vocabularyService = VocabularyServiceFactory.getInstance().getVocabularyService();
    }

    /**
     * Override the abstract 'perform' method to either distribute, or perform single-item
     * depending on configuration. By default, the task is *not* distributed, since that could be unsafe
     * and the original purpose of this task is to essentially implement a "Register DOI" button on the Edit Item page.
     * @param dso DSpaceObject for which to register a DOI (must be item)
     * @return status indicator
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        // Check distribution configuration
        if (distributed) {
            // This task is configured for distributed use. Call distribute() and let performItem handle
            // the main processing.
            distribute(dso);
        } else {
            // This task is NOT configured for distributed use (default). Instead process a single item directly
            if (dso instanceof Item) {
                Item item = (Item) dso;
                try {
                    vocabularyService.performMetadataCopy(Curator.curationContext(), item);
                } catch (SQLException | AuthorizeException e) {
                    log.error("Error", e);
                }
            } else {
                log.warn("DOI registration attempted on non-item DSpace Object: " + dso.getID());
            }
            return status;
        }
        return status;
    }

    /**
     * This is called when the task is distributed (ie. called on a set of items or over a whole structure)
     * @param item the DSpace Item
     */
    @Override
    protected void performItem(Item item) {
        try {
            vocabularyService.performMetadataCopy(Curator.curationContext(), item);
        } catch (SQLException | IOException | AuthorizeException e) {
            log.error("Error", e);
        }
    }

}
