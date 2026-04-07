/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translation2publication.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.translation2publication.Translation2Publication;
import org.dspace.translation2publication.Translation2PublicationId;

/**
 * Database Access Object interface class for the Translation2Publication object.
 * The implementation of this class is responsible for all database calls for the Translation2Publication object and is
 * autowired by spring. This class should only be accessed from a single service and should never be exposed outside
 * of the API
 *
 * @author [Your Name]
 */
public interface Translation2PublicationDAO extends GenericDAO<Translation2Publication> {

    /**
     * Find a Translation2Publication by its composite ID
     *
     * @param context The relevant DSpace Context
     * @param id      The composite ID of the Translation2Publication
     * @return The Translation2Publication or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    Translation2Publication findByID(Context context, Translation2PublicationId id) throws SQLException;

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
     * @param context  The relevant DSpace Context
     * @param offset   The offset for pagination
     * @param limit    The limit for pagination
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
    int countRows(Context context) throws SQLException;
}

