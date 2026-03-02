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
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
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
 * This will create a new item with all metadata and relationships from the source item,
 * but without any bundles or bitstreams. The new item will be placed in the same collection
 * as the source item.
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
@RequestMapping("/api/" + ItemRest.CATEGORY + "/" + ItemRest.PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID
        + "/clone")
public class ItemCloneController {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ItemCloneController.class);

    @Autowired
    ConverterService converter;

    @Autowired
    ItemService itemService;

    @Autowired
    WorkspaceItemService workspaceItemService;

    @Autowired
    InstallItemService installItemService;

    @Autowired
    RelationshipService relationshipService;

    @Autowired
    Utils utils;

    /**
     * Method to clone an Item with the given UUID in the URL. This will create a new Item with all
     * metadata and relationships from the source item, but without any bundles or bitstreams.
     * The new item will be placed in the same collection as the source item.
     *
     * Only administrators can clone items.
     *
     * @param uuid The UUID of the item to clone
     * @param request The HTTP request
     * @return The cloned ItemResource
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RepresentationModel<?>> cloneItem(@PathVariable UUID uuid,
                                                            HttpServletRequest request)
            throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        log.error("Cloning item with id: " + uuid);

        // Find the source item
        Item sourceItem = itemService.find(context, uuid);

        if (sourceItem == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }

        // Get the owning collection of the source item
        Collection collection = sourceItem.getOwningCollection();
        if (collection == null) {
            throw new IllegalStateException("Source item does not have an owning collection");
        }

        // Create a workspace item and get the new item
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item clonedItem = workspaceItem.getItem();

        log.info("Created workspace item with id: " + workspaceItem.getID() + " for cloned item");

        // Copy all metadata from source item to cloned item
        List<MetadataValue> sourceMetadata = itemService.getMetadata(
            sourceItem, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        for (MetadataValue metadataValue : sourceMetadata) {
            itemService.addMetadata(
                context,
                clonedItem,
                metadataValue.getMetadataField().getMetadataSchema().getName(),
                metadataValue.getMetadataField().getElement(),
                metadataValue.getMetadataField().getQualifier(),
                metadataValue.getLanguage(),
                metadataValue.getValue(),
                metadataValue.getAuthority(),
                metadataValue.getConfidence()
            );
        }

        log.info("Copied " + sourceMetadata.size() + " metadata values from source item to cloned item");

        // Set item properties
        //clonedItem.setArchived(true);
        clonedItem.setOwningCollection(collection);
        clonedItem.setDiscoverable(sourceItem.isDiscoverable());

        log.info("Installing cloned item from workspace item with id: " + workspaceItem.getID());

        // Install the item
        Item installedItem = installItemService.installItem(context, workspaceItem);

        log.info("Installed cloned item with id: " + installedItem.getID() + " from workspace item with id: " + workspaceItem.getID());

        // Copy relationships from source item to cloned item
        List<Relationship> sourceRelationships = relationshipService.findByItem(context, sourceItem);

        for (Relationship sourceRelationship : sourceRelationships) {
            Item leftItem;
            Item rightItem;
            int leftPlace = sourceRelationship.getLeftPlace();
            int rightPlace = -1; // rightPlace is commented out in Relationship class, use -1 to append

            // Determine which side of the relationship is the source item
            if (sourceRelationship.getLeftItem().equals(sourceItem)) {
                leftItem = installedItem;
                rightItem = sourceRelationship.getRightItem();
            } else {
                leftItem = sourceRelationship.getLeftItem();
                rightItem = installedItem;
            }

            // Create the new relationship with leftward and rightward values
            relationshipService.create(
                context,
                leftItem,
                rightItem,
                sourceRelationship.getRelationshipType(),
                leftPlace,
                rightPlace,
                sourceRelationship.getLeftwardValue(),
                sourceRelationship.getRightwardValue()
            );
        }

        log.info("Copied " + sourceRelationships.size() + " relationships from source item to cloned item");

        context.commit();

        log.info("Successfully cloned item with id: " + sourceItem.getID()
            + " to new item with id: " + installedItem.getID());

        // Convert to REST resource and return
        ItemResource itemResource = converter.toResource(
            converter.toRest(installedItem, utils.obtainProjection()));
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), itemResource);
    }
}
