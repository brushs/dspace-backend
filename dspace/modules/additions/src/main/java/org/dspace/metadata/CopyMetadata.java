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
        log.info("Starting Copy Metadata Process ");

        // setup Context
        Context context = new Context();

        // Started from commandline, don't use the authentication system.
        context.turnOffAuthorisationSystem();

        CopyMetadata metadataCopier = new CopyMetadata(context);
        // run command line interface
        runCLI(context, metadataCopier, args);

        try {
            context.complete();
            log.info("Finished Copy Metadata Process ");
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
        options.addOption("l", "limit", true,
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
            try {
                String limitStr = line.getOptionValue("limit");
                log.info("Found limit parameter:" + limitStr);
                limit = Integer.parseInt(limitStr);
            } catch (Exception e) {
                log.warn("Error getting limit, using default:" + DEFAULT_LIMIT);
            }
        }

        metadataCopier.process(context, limit);

    }

    private void process(Context context, int limit) throws SQLException, IOException, AuthorizeException {

        SimpleDateFormat fullIso2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        // This whole process is supposed to permit users to only enter in subjects in a single language but
        // Then make them available/visible bilingually (as opposed to just searchable)

        // To determine whether we need to process an item by count of the subject records is unfortunately complicated

        // Normally, there would be 2 : 1 ratio between the curated fields (bilingual, visible fields to public taken from
        // our vocabularies/terms after processing the raw fields from user input). This ratio is broken by at least 3 gotchas

        // 1 - Several vocabularies have the same French term as the translation for different English terms. This means that
        //      if a user adds the French term the job should add the corresponding English term. But we don't know which
        //      of the two English terms to add, so we would have to add both. So now we have more curated fields than we should (2x + 1 : x)
        // 2 - For the same reason, it's possible the user adds both of the English terms with the duplicate French term. Our job
        //      would add the French term, but only once as we check to see if it already exists. Now we have fewer curated fields
        //      than we should. (2x - 1 : x)
        // 3 - Users may choose to enter in the English and French values in the raw subject data. In this case we would only 1 record
        //      record in each language in the curated subject field (x : x)

        // The above problems noted, we will not let perfect be the enemy of pretty good. This logic should work 99+% of the time

        // If issues arise, no bad data is added, the offending items just take up space in our batch
        // If too much space is taken up (maybe check periodically) you could perform some manual cleanup (if it's issue #3)
        // Or perhaps add an new hidden field "copymetadataexclude" and add that to the view and retrieval logic

        log.info("Processing Items with Missing Types");
        List<MetadataLanguageSummary> itemsToProcess = vocabularyService.getItemsForMetadataProcessingByType(context, limit);
        /*
        log.info("Found " + itemsToProcess.size() + " items");
        limit = limit - itemsToProcess.size();

        for (MetadataLanguageSummary mls : itemsToProcess) {
            log.info("Processing ID: " + mls.getId() + " Last Modified: " + fullIso2.format(mls.getLastModified()));
            vocabularyService.performMetadataCopy(context, itemService.find(context, mls.getId()));
            context.commit();
        }

        if (limit <= 0) {
            log.info("Reached Limit, exiting process");
            return;
        }

        log.info("Processing Items with No MetadataProcessDate");
        itemsToProcess = vocabularyService.getItemsForMetadataProcessingNoMPD(context, limit);
        log.info("Found " + itemsToProcess.size() + " items");
        limit = limit - itemsToProcess.size();

        for (MetadataLanguageSummary mls : itemsToProcess) {
            log.info("Processing ID: " + mls.getId() + " Last Modified: " + fullIso2.format(mls.getLastModified()));
            vocabularyService.performMetadataCopy(context, itemService.find(context, mls.getId()));
            context.commit();
        }

        if (limit <= 0) {
            log.info("Reached Limit, exiting process");
            return;
        }
        */
        log.info("Processing Items by MetadataProcessDate");
        itemsToProcess = vocabularyService.getItemsForMetadataProcessingByMPD(context, limit);
        log.info("Found " + itemsToProcess.size() + " items");

        for (MetadataLanguageSummary mls : itemsToProcess) {
            log.info("Processing ID: " + mls.getId() + " Last Modified: " + fullIso2.format(mls.getLastModified()));
            vocabularyService.performMetadataCopy(context, itemService.find(context, mls.getId()));
            context.commit();
        }
    }


}
