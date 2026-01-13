/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translationrequest.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.translationrequest.TranslationRequest;

/**
 * Database Access Object interface class for the TranslationRequest object.
 * The implementation of this class is responsible for all database calls for the TranslationRequest object and is
 * autowired by spring. This class should only be accessed from a single service and should never be exposed outside
 * of the API
 *
 * @author [Your Name]
 */
public interface TranslationRequestDAO extends GenericDAO<TranslationRequest> {

    /**
     * Find a TranslationRequest by its ID
     *
     * @param context The relevant DSpace Context
     * @param id      The ID of the TranslationRequest
     * @return The TranslationRequest or null if not found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    TranslationRequest findByID(Context context, Integer id) throws SQLException;

    /**
     * Find TranslationRequests by publication GUID
     *
     * @param context         The relevant DSpace Context
     * @param publicationGUID The publication GUID to search for
     * @return List of TranslationRequests matching the GUID
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<TranslationRequest> findByPublicationGUID(Context context, String publicationGUID) throws SQLException;

    /**
     * Find all TranslationRequests with pagination
     *
     * @param context  The relevant DSpace Context
     * @param offset   The offset for pagination
     * @param limit    The limit for pagination
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
    int countRows(Context context) throws SQLException;
}

