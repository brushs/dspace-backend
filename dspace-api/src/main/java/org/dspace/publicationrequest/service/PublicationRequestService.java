/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.publicationrequest.PublicationRequest;

/**
 * Service interface class for the PublicationRequest object.
 * The implementation of this class is responsible for all business logic calls
 * for the PublicationRequest object and is autowired by Spring.
 *
 * @author [Your Name]
 */
public interface PublicationRequestService {

    /**
     * Create a new PublicationRequest
     *
     * @param context The relevant DSpace Context
     * @return The newly created PublicationRequest
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    PublicationRequest create(Context context) throws SQLException, AuthorizeException;

    /**
     * Find a PublicationRequest by its ID
     *
     * @param context The relevant DSpace Context
     * @param id      The ID of the PublicationRequest
     * @return The PublicationRequest or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    PublicationRequest find(Context context, Integer id) throws SQLException;

    /**
     * Update an existing PublicationRequest
     *
     * @param context            The relevant DSpace Context
     * @param publicationRequest The PublicationRequest to update
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    void update(Context context, PublicationRequest publicationRequest) throws SQLException, AuthorizeException;

    /**
     * Update an existing PublicationRequest without authorization checks.
     * This should only be used internally for public creation of new requests.
     *
     * @param context            The relevant DSpace Context
     * @param publicationRequest The PublicationRequest to update
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    void updateWithoutAuthCheck(Context context, PublicationRequest publicationRequest) throws SQLException;

    /**
     * Delete a PublicationRequest
     *
     * @param context            The relevant DSpace Context
     * @param publicationRequest The PublicationRequest to delete
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    void delete(Context context, PublicationRequest publicationRequest) throws SQLException, AuthorizeException;

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
     * @param context The relevant DSpace Context
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
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
    int countTotal(Context context) throws SQLException;

    /**
     * Find PublicationRequests by title (searches both English and French titles)
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
     * Count PublicationRequests by title (searches both English and French titles)
     *
     * @param context The relevant DSpace Context
     * @param title   The title to search for (case-insensitive partial match)
     * @return The count of matching PublicationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByTitle(Context context, String title) throws SQLException;

    /**
     * Find PublicationRequests by translation request ID
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
     * Count PublicationRequests by translation request ID
     *
     * @param context              The relevant DSpace Context
     * @param translationRequestId The translation request ID to search for
     * @return The count of matching PublicationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByTranslationRequestId(Context context, Integer translationRequestId) throws SQLException;

    /**
     * Find PublicationRequests by status
     *
     * @param context The relevant DSpace Context
     * @param status  The status to search for
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination (-1 for all)
     * @return List of PublicationRequests matching the status
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<PublicationRequest> findByStatus(Context context, int status, int offset, int limit) throws SQLException;

    /**
     * Count PublicationRequests by status
     *
     * @param context The relevant DSpace Context
     * @param status  The status to search for
     * @return The count of matching PublicationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByStatus(Context context, int status) throws SQLException;
}

