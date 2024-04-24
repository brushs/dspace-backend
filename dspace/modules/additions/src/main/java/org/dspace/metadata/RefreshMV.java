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
import org.dspace.content.MetadataLanguageSummary;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.factory.VocabularyServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class RefreshMV {
    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RefreshMV.class);

    private final Context context;

    protected VocabularyService vocabularyService;

    public RefreshMV(Context context) {
        this.context = context;
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

        RefreshMV refreshMV = new RefreshMV(context);
        // run command line interface
        runCLI(context, refreshMV, args);

        try {
            context.complete();
        } catch (SQLException sqle) {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

    }

    public static void runCLI(Context context, RefreshMV refreshMV, String[] args)
            throws SQLException, IOException, AuthorizeException {

        refreshMV.process(context);
    }

    private void process(Context context) {
        vocabularyService.refreshMetadataSummaryMatView(context);
    }

}
