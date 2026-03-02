/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemCloningService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for cloning items.
 * This service handles the creation of a cloned item with all metadata and relationships
 * from the source item, but without any bundles or bitstreams.
 *
 * @author DSpace Community
 */
public class ItemCloningServiceImpl implements ItemCloningService {

    private static final Logger log = LogManager.getLogger(ItemCloningServiceImpl.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected WorkspaceItemService workspaceItemService;

    @Autowired
    protected InstallItemService installItemService;

    @Autowired
    protected RelationshipService relationshipService;

    @Override
    public Item cloneItem(Context context, Item sourceItem) throws SQLException, AuthorizeException {
        log.info("Starting clone process for item with id: {}", sourceItem.getID());

        try {
            // Get the owning collection of the source item
            Collection collection = sourceItem.getOwningCollection();
            if (collection == null) {
                throw new IllegalStateException("Source item does not have an owning collection");
            }

            // Create a workspace item and get the new item
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            Item clonedItem = workspaceItem.getItem();

            log.info("Created workspace item with id: {} for cloned item", workspaceItem.getID());

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

            log.info("Copied {} metadata values from source item to cloned item", sourceMetadata.size());

            // Set item properties
            clonedItem.setOwningCollection(collection);
            clonedItem.setDiscoverable(sourceItem.isDiscoverable());

            log.info("Installing cloned item from workspace item with id: {}", workspaceItem.getID());

            // Install the item
            Item installedItem = installItemService.installItem(context, workspaceItem);

            log.info("Installed cloned item with id: {} from workspace item with id: {}",
                installedItem.getID(), workspaceItem.getID());

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

            log.info("Copied {} relationships from source item to cloned item", sourceRelationships.size());

            // Update the item to persist changes
            itemService.update(context, installedItem);

            log.info("Successfully cloned item with id: {} to new item with id: {}",
                sourceItem.getID(), installedItem.getID());

            return installedItem;

        } catch (SQLException | AuthorizeException e) {
            log.error("Error cloning item with id: {}", sourceItem.getID(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error cloning item with id: {}", sourceItem.getID(), e);
            throw new RuntimeException("Failed to clone item", e);
        }
    }
}
