/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translationrequest.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.translationrequest.TranslationRequest;

/**
 * Service interface class for the TranslationRequest object.
 * The implementation of this class is responsible for all business logic calls
 * for the TranslationRequest object and is autowired by Spring.
 *
 * @author [Your Name]
 */
public interface TranslationRequestService {

    /**
     * Create a new TranslationRequest
     *
     * @param context The relevant DSpace Context
     * @return The newly created TranslationRequest
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    TranslationRequest create(Context context) throws SQLException, AuthorizeException;

    /**
     * Find a TranslationRequest by its ID
     *
     * @param context The relevant DSpace Context
     * @param id      The ID of the TranslationRequest
     * @return The TranslationRequest or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    TranslationRequest find(Context context, Integer id) throws SQLException;

    /**
     * Update an existing TranslationRequest
     *
     * @param context            The relevant DSpace Context
     * @param translationRequest The TranslationRequest to update
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    void update(Context context, TranslationRequest translationRequest) throws SQLException, AuthorizeException;

    /**
     * Update an existing TranslationRequest without authorization checks.
     * This should only be used internally for public creation of new requests.
     *
     * @param context            The relevant DSpace Context
     * @param translationRequest The TranslationRequest to update
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    void updateWithoutAuthCheck(Context context, TranslationRequest translationRequest) throws SQLException;

    /**
     * Delete a TranslationRequest
     *
     * @param context            The relevant DSpace Context
     * @param translationRequest The TranslationRequest to delete
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user does not have permission to perform a
     *                            particular action.
     */
    void delete(Context context, TranslationRequest translationRequest) throws SQLException, AuthorizeException;

    /**
     * Find TranslationRequests by publication UUID
     *
     * @param context         The relevant DSpace Context
     * @param publicationUUID The publication UUID to search for
     * @return List of TranslationRequests matching the UUID
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<TranslationRequest> findByPublicationUUID(Context context, String publicationUUID) throws SQLException;

    /**
     * Find a TranslationRequest by bitstream UUID and publication UUID
     *
     * @param context         The relevant DSpace Context
     * @param bitstreamUUID   The bitstream UUID to search for
     * @param publicationUUID The publication UUID to search for
     * @return The TranslationRequest or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    TranslationRequest findByBitstreamUUIDAndPublicationUUID(Context context, String bitstreamUUID,
                                                              String publicationUUID) throws SQLException;

    /**
     * Find all TranslationRequests with pagination
     *
     * @param context The relevant DSpace Context
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
     * @return List of TranslationRequests
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<TranslationRequest> findAll(Context context, int offset, int limit) throws SQLException;

    /**
     * Count total number of TranslationRequests
     *
     * @param context The relevant DSpace Context
     * @return The total count
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Find TranslationRequests by publication title (searches both English and French titles)
     *
     * @param context The relevant DSpace Context
     * @param title   The title to search for (case-insensitive partial match)
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
     * @return List of TranslationRequests matching the title
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<TranslationRequest> findByPublicationTitle(Context context, String title, int offset, int limit)
        throws SQLException;

    /**
     * Count TranslationRequests by publication title
     *
     * @param context The relevant DSpace Context
     * @param title   The title to search for (case-insensitive partial match)
     * @return The total count
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByPublicationTitle(Context context, String title) throws SQLException;

    /**
     * Find TranslationRequests by user email (from related publication request)
     *
     * @param context The relevant DSpace Context
     * @param email   The email address to search for
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
     * @return List of TranslationRequests matching the email
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<TranslationRequest> findByUserEmail(Context context, String email, int offset, int limit)
        throws SQLException;

    /**
     * Count TranslationRequests by user email
     *
     * @param context The relevant DSpace Context
     * @param email   The email address to search for
     * @return The total count
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByUserEmail(Context context, String email) throws SQLException;

    /**
     * Find TranslationRequests by status
     *
     * @param context The relevant DSpace Context
     * @param status  The status to search for
     * @param offset  The offset for pagination
     * @param limit   The limit for pagination
     * @return List of TranslationRequests matching the status
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<TranslationRequest> findByStatus(Context context, int status, int offset, int limit)
        throws SQLException;

    /**
     * Count TranslationRequests by status
     *
     * @param context The relevant DSpace Context
     * @param status  The status to search for
     * @return The total count
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countByStatus(Context context, int status) throws SQLException;
}

