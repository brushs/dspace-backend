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

public class CopyVocabularyMetadata extends AbstractCurationTask {
    // Curation task status
    private int status = Curator.CURATE_SUCCESS;
    // The distributed boolean has a default value of 'false' for safest operation
    private boolean distributed = false;
    // Prefix for configuration module
    private static final String PLUGIN_PREFIX = "copy-vocab";
    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CopyVocabularyMetadata.class);

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
                    performMetadataCopy(item);
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
            performMetadataCopy(item);
        } catch (SQLException | IOException | AuthorizeException e) {
            log.error("Error", e);
        }
    }

    /**
     * Shared 'perform' code between perform() and performItem() - a curation wrapper for the register() method
     * @param item the item
     */
    private void performMetadataCopy(Item item) throws SQLException, IOException, AuthorizeException {
        // If not recently updated, return
        // TODO provide CLI option to skip this check (not sure possible), parameterize minusDays in config
        LocalDate lastModified = item.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate compareDate = LocalDate.now().minusDays(2);

        if (lastModified.compareTo(compareDate) < 0) {
            return;
        }

        // TODO pull from config
        Map<String, Integer> fieldsToProcess = new HashMap<String, Integer>();
        fieldsToProcess.put("dc.subject.cfs", 5);
        fieldsToProcess.put("dc.subject.gc", 2);
        fieldsToProcess.put("dc.subject.broad", 3);
        fieldsToProcess.put("dc.subject.geoscan", 1);
        fieldsToProcess.put("dc.subject.descriptor", 4);
        fieldsToProcess.put("dc.type", 6);

        for (String metadataField : fieldsToProcess.keySet()) {
            // Get Item metadata
            List<MetadataValue> mdvs = itemService.getMetadataByMetadataString(item, metadataField);

            Integer vocabularyId = fieldsToProcess.get(metadataField);

            for (MetadataValue mdv : mdvs) {
                log.info("Processing Value: " + mdv.getValue());
                // TODO Limit check to specific vocabulary based on config?
                // Check to see if any terms are matched
                List<Term> terms = vocabularyService.findByName(Curator.curationContext(), mdv.getValue(), vocabularyId);

                if (terms != null && terms.size() > 0) {
                    Map<String, String> mappedMetadataFields = new HashMap<>();
                    mappedMetadataFields.put(metadataField + "_en", terms.get(0).getNameEn());
                    mappedMetadataFields.put(metadataField + "_fr", terms.get(0).getNameFr());

                    log.info("Found Term");
                    for (Map.Entry<String, String> mappedMetadataField : mappedMetadataFields.entrySet()) {
                        // Check to see if mapped terms already exist
                        List<MetadataValue> mappedMdvs = itemService.getMetadataByMetadataString(item, mappedMetadataField.getKey());

                        boolean mappedValueExists = true;
                        if (mappedMdvs == null || mappedMdvs.size() == 0) {
                            mappedValueExists = false;
                        } else {
                            List<String> mdvValues = mappedMdvs.stream()
                                    .map(MetadataValue::getValue)
                                    .collect(Collectors.toList());

                            mappedValueExists = mdvValues.stream().anyMatch(value -> value.equals(mappedMetadataField.getValue()));

                        }

                        if (!mappedValueExists) {
                            log.info("Adding new value");
                            // Copy to new metadata field
                            String[] tokens = mappedMetadataField.getKey().split("\\.");
                            itemService.addMetadata(Curator.curationContext(), item, tokens[0], tokens[1], tokens.length == 3 ? tokens[2] : null,
                                    mappedMetadataField.getKey().endsWith("_en") ? "en" : "fr", mappedMetadataField.getValue());
                            itemService.updateLastModified(Curator.curationContext(), item);
                        }
                    }
                }
                else {
                    // No matching text was found in Vocabulary
                    log.warn("Subject not found in Vocabulary - ID: " + item.getID() + " Val - " + mdv.getValue());
                }
            }
        }
    }
}
