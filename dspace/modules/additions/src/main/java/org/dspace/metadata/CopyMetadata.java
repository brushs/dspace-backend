/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metadata;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.factory.VocabularyServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.doi.DOIOrganiser;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class CopyMetadata {
    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CopyMetadata.class);

    private final Context context;

    protected VocabularyService vocabularyService;

    protected ItemService itemService;

    private static int DEFAULT_LIMIT = 1000;

    public CopyMetadata(Context context) {
        this.context = context;
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.vocabularyService = VocabularyServiceFactory.getInstance().getVocabularyService();
    }

    /**
     * Main command-line runner method as with other DSpace launcher commands
     * @param args  - the command line arguments to parse as parameters
     */
    public static void main(String[] args) throws SQLException, AuthorizeException, IOException {
        log.debug("Starting Copy Metadata Process ");

        // setup Context
        Context context = new Context();

        // Started from commandline, don't use the authentication system.
        context.turnOffAuthorisationSystem();

        CopyMetadata metadataCopier = new CopyMetadata(context);
        // run command line interface
        runCLI(context, metadataCopier, args);

        try {
            context.complete();
        } catch (SQLException sqle) {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

    }

    public static void runCLI(Context context, CopyMetadata metadataCopier, String[] args)
            throws SQLException, IOException, AuthorizeException {
        // initialize options
        Options options = new Options();

        options.addOption("h", "help", false, "Help");
        options.addOption("l", "limit", false,
                "Limit of items to process in a run ");

        // initialize parser
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        HelpFormatter helpformater = new HelpFormatter();

        try {
            line = parser.parse(options, args);
        } catch (ParseException ex) {
            log.fatal(ex);
            System.exit(1);
        }

        // process options
        // user asks for help
        if (line.hasOption('h') || 0 == line.getOptions().length) {
            helpformater.printHelp("\nMetadata Copier\n", options);
        }

        int limit = DEFAULT_LIMIT;
        if (line.hasOption('l')) {
            //TODO
            // get limit from CLI
        }

        metadataCopier.process(context, limit);

    }

    private void process(Context context, int limit) throws SQLException, IOException, AuthorizeException {
        List<MetadataLanguageSummary> itemsToProcess = vocabularyService.getItemsForMetadataProcessing(context, limit);

        SimpleDateFormat fullIso2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        for (MetadataLanguageSummary mls : itemsToProcess) {
            log.info("Processing ID: " + mls.getId() + " Type Count: " + mls.getTypeCount()
                + " Type En Count: " + mls.getTypeEnCount() + " Subject Count: " + mls.getSubjectCount()
                + " Subject En Count: " + mls.getSubjectEnCount() + " Modified: " +
                    fullIso2.format(mls.getLastModified()));
            performMetadataCopy(itemService.find(context, mls.getId()));
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
                                    .filter(x -> x != null)
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
