/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translation2publication.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.translation2publication.Translation2Publication;
import org.dspace.translation2publication.Translation2PublicationId;

/**
 * Service interface class for the Translation2Publication object.
 * The implementation of this class is responsible for all business logic calls
 * for the Translation2Publication object and is autowired by Spring.
 *
 * @author [Your Name]
 */
public interface Translation2PublicationService {

    /**
     * Create a new Translation2Publication
     *
     * @param context The relevant DSpace Context
     * @return The newly created Translation2Publication
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    Translation2Publication create(Context context) throws SQLException, AuthorizeException;

    /**
     * Find a Translation2Publication by its composite ID
     *
     * @param context The relevant DSpace Context
     * @param id      The composite ID of the Translation2Publication
     * @return The Translation2Publication or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    Translation2Publication find(Context context, Translation2PublicationId id) throws SQLException;

    /**
     * Find a Translation2Publication by translation request ID and publication request ID
     *
     * @param context               The relevant DSpace Context
     * @param translationRequestId  The translation request ID
     * @param publicationRequestId  The publication request ID
     * @return The Translation2Publication or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    Translation2Publication find(Context context, Integer translationRequestId, Integer publicationRequestId)
        throws SQLException;

    /**
     * Update an existing Translation2Publication
     *
     * @param context               The relevant DSpace Context
     * @param translation2Publication The Translation2Publication to update
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    void update(Context context, Translation2Publication translation2Publication)
        throws SQLException, AuthorizeException;

    /**
     * Delete a Translation2Publication
     *
     * @param context               The relevant DSpace Context
     * @param translation2Publication The Translation2Publication to delete
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    void delete(Context context, Translation2Publication translation2Publication)
        throws SQLException, AuthorizeException;

    /**
     * Find Translation2Publication entries by translation request ID
     *
     * @param context              The relevant DSpace Context
     * @param translationRequestId The translation request ID to search for
     * @return List of Translation2Publication entries matching the translation request ID
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<Translation2Publication> findByTranslationRequestId(Context context, Integer translationRequestId)
        throws SQLException;

    /**
     * Find Translation2Publication entries by publication request ID
     *
     * @param context               The relevant DSpace Context
     * @param publicationRequestId The publication request ID to search for
     * @return List of Translation2Publication entries matching the publication request ID
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<Translation2Publication> findByPublicationRequestId(Context context, Integer publicationRequestId)
        throws SQLException;

    /**
     * Find all Translation2Publication entries with pagination
     *
     * @param context The relevant DSpace Context
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
     * @return List of Translation2Publication entries
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<Translation2Publication> findAll(Context context, int offset, int limit) throws SQLException;

    /**
     * Count total number of Translation2Publication entries
     *
     * @param context The relevant DSpace Context
     * @return The total count
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countTotal(Context context) throws SQLException;
}

