/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface for cloning items.
 * This service handles the creation of a cloned item with all metadata and relationships
 * from the source item, but without any bundles or bitstreams.
 *
 * @author DSpace Community
 */
public interface ItemCloningService {

    /**
     * Clone an item with all its metadata and relationships, but without bundles or bitstreams.
     * The cloned item will be placed in the same collection as the source item.
     *
     * @param context The DSpace context
     * @param sourceItem The item to clone
     * @return The cloned and installed item
     * @throws SQLException if database error occurs
     * @throws AuthorizeException if authorization error occurs
     */
    Item cloneItem(Context context, Item sourceItem) throws SQLException, AuthorizeException;
}
