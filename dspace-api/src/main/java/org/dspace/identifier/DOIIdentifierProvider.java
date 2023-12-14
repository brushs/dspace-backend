package org.dspace.identifier;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.doi.DOIIdentifierException;

import java.sql.SQLException;

public interface DOIIdentifierProvider {

    Integer TO_BE_REGISTERED = 1;
    Integer TO_BE_RESERVED = 2;
    Integer IS_REGISTERED = 3;
    Integer IS_RESERVED = 4;
    Integer UPDATE_RESERVED = 5;
    Integer UPDATE_REGISTERED = 6;
    Integer UPDATE_BEFORE_REGISTRATION = 7;
    Integer TO_BE_DELETED = 8;
    Integer DELETED = 9;
    Integer PENDING = 10;
    Integer CALLBACK_PROCESSING_PENDING = 11;
    Integer ERROR = 12;

    String CFG_PREFIX = "identifier.doi.prefix";
    String CFG_NAMESPACE_SEPARATOR = "identifier.doi.namespaceseparator";

    // Metadata field name elements
    String MD_SCHEMA = "dc";
    String DOI_ELEMENT = "identifier";
    String DOI_QUALIFIER = "doi";

    String register(Context context, DSpaceObject dso, boolean skipFilter)
            throws IdentifierException;

    void registerOnline(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException, IllegalArgumentException, SQLException;

    void reserveOnline(Context context, DSpaceObject dso, String identifier, boolean skipFilter)
            throws IdentifierException, IllegalArgumentException, SQLException;

    void updateMetadata(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException, IllegalArgumentException, SQLException;

    void updateMetadataOnline(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException, SQLException;

    String mint(Context context, DSpaceObject dso, boolean skipFilter) throws IdentifierException;

    void deleteOnline(Context context, String identifier) throws DOIIdentifierException;

    String lookup(Context context, DSpaceObject dso)
            throws IdentifierNotFoundException, IdentifierNotResolvableException;

    void processCallback(Context context, String batchId, String retrieveUrl) throws SQLException;

    void saveDOIToObject(Context context, DSpaceObject dso, String doi)
            throws SQLException, AuthorizeException, IdentifierException;
}
