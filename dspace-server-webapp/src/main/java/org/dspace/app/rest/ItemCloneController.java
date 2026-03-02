/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.app.rest.scripts.handler.impl.RestDSpaceRunnableHandler;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to clone an item with the given UUID in the URL.
 * This will create an asynchronous curation task to clone the item with all metadata
 * and relationships from the source item, but without any bundles or bitstreams.
 * The new item will be placed in the same collection as the source item.
 *
 * The cloning is done asynchronously via a curation task and will appear in the Processes list.
 *
 * Usage: POST /api/core/items/<:uuid>/clone
 *
 * Example:
 * <pre>
 * {@code
 * curl -X POST https://<dspace.server.url>/api/core/items/1911e8a4-6939-490c-b58b-a5d70f8d91fb/clone
 *  -H 'Authorization: Bearer eyJhbGciOiJI...'
 * }
 * </pre>
 *
 * @author DSpace Community
 */
@RestController
@RequestMapping("/api/" + org.dspace.app.rest.model.ItemRest.CATEGORY + "/" +
        org.dspace.app.rest.model.ItemRest.PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID
        + "/clone")
public class ItemCloneController {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ItemCloneController.class);

    @Autowired
    ConverterService converter;

    @Autowired
    ItemService itemService;

    @Autowired
    ScriptService scriptService;

    @Autowired
    Utils utils;

    /**
     * Method to initiate the cloning of an Item with the given UUID in the URL.
     * This will start an asynchronous curation task to clone the item.
     * The cloning process will appear in the Processes list and can be monitored there.
     *
     * Only administrators can clone items.
     *
     * @param uuid The UUID of the item to clone
     * @param request The HTTP request
     * @return A Process resource representing the cloning task
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RepresentationModel<?>> cloneItem(@PathVariable UUID uuid,
                                                            HttpServletRequest request)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        log.info("Initiating clone task for item with UUID: {}", uuid);

        // Find the source item to verify it exists
        Item sourceItem = itemService.find(context, uuid);

        if (sourceItem == null) {
            throw new ResourceNotFoundException("Could not find item with UUID: " + uuid);
        }

        // Get the item's handle for the curation task
        String handle = sourceItem.getHandle();
        if (handle == null) {
            throw new IllegalStateException("Item does not have a handle. Only archived items can be cloned.");
        }

        log.info("Starting asynchronous clone task for item with handle: {}", handle);

        try {
            // Get the curate script configuration
            ScriptConfiguration scriptConfig = scriptService.getScriptConfiguration("curate");
            if (scriptConfig == null) {
                throw new IllegalStateException("Could not find 'curate' script configuration");
            }

            // Verify user is authorized to execute the script
            if (!scriptConfig.isAllowedToExecute(context)) {
                throw new AuthorizeException("Current user is not authorized to execute curation tasks");
            }

            // Create parameters for the curation script
            List<DSpaceCommandLineParameter> parameters = new ArrayList<>();
            parameters.add(new DSpaceCommandLineParameter("-t", "cloneitem"));
            parameters.add(new DSpaceCommandLineParameter("-i", handle));

            // Create the runnable handler which will create the process
            RestDSpaceRunnableHandler runnableHandler = new RestDSpaceRunnableHandler(
                context.getCurrentUser(),
                scriptConfig.getName(),
                parameters,
                new HashSet<>(context.getSpecialGroups())
            );

            // Create the DSpaceRunnable instance
            DSpaceRunnable runnable = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfig);

            // Prepare arguments array
            List<String> args = new ArrayList<>();
            for (DSpaceCommandLineParameter param : parameters) {
                args.add(param.getName());
                if (param.getValue() != null) {
                    args.add(param.getValue());
                }
            }

            // Initialize the runnable
            try {
                runnable.initialize(args.toArray(new String[0]), runnableHandler, context.getCurrentUser());

                // Schedule the process for execution
                runnableHandler.schedule(runnable);

                log.info("Successfully scheduled clone task process for item: {}", handle);

                // Get the process that was created by the handler
                org.dspace.scripts.Process process = runnableHandler.getProcess(context);

                // Convert to REST resource and return
                ProcessRest processRest = converter.toRest(process, utils.obtainProjection());
                ProcessResource processResource = converter.toResource(processRest);

                return ControllerUtils.toResponseEntity(HttpStatus.ACCEPTED, new HttpHeaders(), processResource);

            } catch (ParseException e) {
                log.error("Failed to parse arguments for clone task: {}", e.getMessage(), e);
                runnable.printHelp();
                throw new RuntimeException("Failed to parse clone task arguments: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error starting clone task for item with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to start clone task: " + e.getMessage(), e);
        }
    }
}
