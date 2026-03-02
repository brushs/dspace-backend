/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemCloningService;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.utils.DSpace;

/**
 * Curation task to clone an item with all its metadata and relationships,
 * but without bundles or bitstreams. The cloned item will be placed in the
 * same collection as the source item.
 *
 * This task should only be run on individual items (not distributed across collections/communities).
 *
 * @author DSpace Community
 */
public class CloneItem extends AbstractCurationTask {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CloneItem.class);

    private ItemCloningService itemCloningService;

    /**
     * Initialize the curation task
     */
    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        // Get service by type since it doesn't have an explicit bean name
        DSpace dspace = new DSpace();
        java.util.List<ItemCloningService> services = dspace.getServiceManager()
            .getServicesByType(ItemCloningService.class);

        if (services == null || services.isEmpty()) {
            throw new IOException("Failed to initialize ItemCloningService - no service implementation found");
        }

        itemCloningService = services.get(0);
        log.debug("Successfully initialized ItemCloningService");
    }

    /**
     * Perform the cloning task on the given DSpace object.
     * This task only works on Items.
     *
     * @param dso the DSpace object (must be an Item)
     * @return status code
     * @throws IOException if an error occurs
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        int status;

        if (dso == null) {
            setResult("DSpaceObject is null");
            status = Curator.CURATE_FAIL;
            return status;
        }

        if (!(dso instanceof Item)) {
            String message = "CloneItem task can only be run on Items. Skipping " + dso.getType();
            setResult(message);
            log.warn(message);
            status = Curator.CURATE_SKIP;
            return status;
        }

        Item sourceItem = (Item) dso;

        try {
            log.info("Starting clone task for item with UUID: {}", sourceItem.getID());

            // Clone the item using the service
            Item clonedItem = itemCloningService.cloneItem(Curator.curationContext(), sourceItem);

            String message = String.format(
                "Successfully cloned item %s (UUID: %s) to new item with UUID: %s",
                sourceItem.getHandle() != null ? sourceItem.getHandle() : "workspace item",
                sourceItem.getID(),
                clonedItem.getID()
            );

            setResult(message);
            log.info(message);

            // Report the cloned item's details
            report(message);
            report("Cloned item UUID: " + clonedItem.getID());
            if (clonedItem.getHandle() != null) {
                report("Cloned item handle: " + clonedItem.getHandle());
            }

            status = Curator.CURATE_SUCCESS;

        } catch (SQLException e) {
            String errorMessage = "Database error while cloning item " + sourceItem.getID() + ": " + e.getMessage();
            setResult(errorMessage);
            log.error(errorMessage, e);
            report(errorMessage);
            status = Curator.CURATE_ERROR;
        } catch (Exception e) {
            String errorMessage = "Error cloning item " + sourceItem.getID() + ": " + e.getMessage();
            setResult(errorMessage);
            log.error(errorMessage, e);
            report(errorMessage);
            status = Curator.CURATE_ERROR;
        }

        return status;
    }
}
