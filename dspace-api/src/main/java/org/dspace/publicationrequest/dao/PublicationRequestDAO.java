/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.publicationrequest.PublicationRequest;

/**
 * Database Access Object interface class for the PublicationRequest object.
 * The implementation of this class is responsible for all database calls for the PublicationRequest object and is
 * autowired by spring. This class should only be accessed from a single service and should never be exposed outside
 * of the API
 *
 * @author [Your Name]
 */
public interface PublicationRequestDAO extends GenericDAO<PublicationRequest> {

    /**
     * Find a PublicationRequest by its ID
     *
     * @param context The relevant DSpace Context
     * @param id      The ID of the PublicationRequest
     * @return The PublicationRequest or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    PublicationRequest findByID(Context context, Integer id) throws SQLException;

    /**
     * Find PublicationRequests by publication UUID
     *
     * @param context         The relevant DSpace Context
     * @param publicationUUID The publication UUID to search for
     * @return List of PublicationRequests matching the UUID
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<PublicationRequest> findByPublicationUUID(Context context, String publicationUUID) throws SQLException;

    /**
     * Find PublicationRequests by user email address
     *
     * @param context          The relevant DSpace Context
     * @param userEmailAddress The email address to search for
     * @return List of PublicationRequests matching the email
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<PublicationRequest> findByUserEmailAddress(Context context, String userEmailAddress) throws SQLException;

    /**
     * Find all PublicationRequests with pagination
     *
     * @param context  The relevant DSpace Context
     * @param offset   The offset for pagination
     * @param limit    The limit for pagination
     * @return List of PublicationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<PublicationRequest> findAll(Context context, int offset, int limit) throws SQLException;

    /**
     * Count total number of PublicationRequests
     *
     * @param context The relevant DSpace Context
     * @return The total count
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countRows(Context context) throws SQLException;

    /**
     * Find PublicationRequests by title (searches both English and French titles in metadatavalue table)
     *
     * @param context The relevant DSpace Context
     * @param title   The title to search for (case-insensitive partial match)
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
     * @return List of PublicationRequests matching the title
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<PublicationRequest> findByTitle(Context context, String title, int offset, int limit) throws SQLException;

    /**
     * Count PublicationRequests by title (searches both English and French titles in metadatavalue table)
     *
     * @param context The relevant DSpace Context
     * @param title   The title to search for (case-insensitive partial match)
     * @return The count of matching PublicationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByTitle(Context context, String title) throws SQLException;

    /**
     * Find PublicationRequests by translation request ID (via translation2publication table join)
     *
     * @param context              The relevant DSpace Context
     * @param translationRequestId The translation request ID to search for
     * @param offset               The offset for pagination
     * @param limit                The limit for pagination
     * @return List of PublicationRequests linked to the translation request
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<PublicationRequest> findByTranslationRequestId(Context context, Integer translationRequestId,
                                                         int offset, int limit) throws SQLException;

    /**
     * Count PublicationRequests by translation request ID (via translation2publication table join)
     *
     * @param context              The relevant DSpace Context
     * @param translationRequestId The translation request ID to search for
     * @return The count of matching PublicationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByTranslationRequestId(Context context, Integer translationRequestId) throws SQLException;
}

