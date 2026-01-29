/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.publicationrequest.dao.PublicationRequestDAO;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.dspace.translation2publication.Translation2Publication;
import org.dspace.translation2publication.service.Translation2PublicationService;
import org.dspace.translationrequest.TranslationRequest;
import org.dspace.translationrequest.service.TranslationRequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the PublicationRequest object. This class is responsible for
 * all business logic calls for the PublicationRequest object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class PublicationRequestServiceImpl implements PublicationRequestService {

    /**
     * log4j logger
     */
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(PublicationRequestServiceImpl.class);

    @Autowired(required = true)
    protected PublicationRequestDAO publicationRequestDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected TranslationRequestService translationRequestService;

    @Autowired(required = true)
    protected Translation2PublicationService translation2PublicationService;

    @Autowired(required = true)
    protected RelationshipService relationshipService;

    @Autowired(required = true)
    protected RelationshipTypeService relationshipTypeService;

    protected PublicationRequestServiceImpl() {
    }

    @Override
    public PublicationRequest create(Context context) throws SQLException, AuthorizeException {
        // Note: Creation is public, no authorization check needed
        // Just create the object without persisting yet - values will be set before saving
        PublicationRequest publicationRequest = new PublicationRequest();
        return publicationRequest;
    }

    @Override
    public PublicationRequest find(Context context, Integer id) throws SQLException {
        return publicationRequestDAO.findByID(context, id);
    }

    @Override
    public void update(Context context, PublicationRequest publicationRequest)
        throws SQLException, AuthorizeException {
        // Only administrators can update
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can update publication requests");
        }
        publicationRequestDAO.save(context, publicationRequest);
        log.info("Updated PublicationRequest with ID: " + publicationRequest.getId());
    }

    @Override
    public void updateWithoutAuthCheck(Context context, PublicationRequest publicationRequest)
        throws SQLException {
        // No authorization check - used for public creation only
        // Check if this is a new entity (no ID) or an existing one
        boolean isNewRequest = (publicationRequest.getId() == null);

        if (isNewRequest) {
            // New entity - set created date
            if (publicationRequest.getCreatedDate() == null) {
                publicationRequest.setCreatedDate(new Date());
            }

            // Create in database
            publicationRequest = publicationRequestDAO.create(context, publicationRequest);
            log.info("Created PublicationRequest with ID: " + publicationRequest.getId());

            // Automatically create translation requests for each bitstream in ORIGINAL bundle
            createTranslationRequestsForOriginalBitstreams(context, publicationRequest);
        } else {
            // Existing entity - use save
            publicationRequestDAO.save(context, publicationRequest);
            log.info("Updated PublicationRequest with ID: " + publicationRequest.getId());
        }
    }

    /**
     * Create translation requests for each bitstream in the ORIGINAL bundle of the publication item
     *
     * @param context The DSpace context
     * @param publicationRequest The publication request that was just created
     */
    private void createTranslationRequestsForOriginalBitstreams(Context context,
                                                                 PublicationRequest publicationRequest) {
        log.info("Creating Translation Request for PR: " + publicationRequest.getId());

        try {
            // Get the item from the publicationUUID
            UUID itemUuid = UUID.fromString(publicationRequest.getPublicationUUID());
            Item item = itemService.find(context, itemUuid);

            log.info("Step 1");

            if (item == null) {
                log.warn("Could not find item with UUID: " + publicationRequest.getPublicationUUID()
                        + " for PublicationRequest ID: " + publicationRequest.getId());
                return;
            }

            log.info("Step 2");

            // Check if the publication is already available in the requested language
            if (isPublicationAvailableInLanguage(context, item, publicationRequest.getLanguage())) {
                log.info("Publication with UUID: " + publicationRequest.getPublicationUUID()
                        + " is already available in requested language: " + publicationRequest.getLanguage()
                        + ". Setting PublicationRequest ID: " + publicationRequest.getId()
                        + " to 'No Translation Needed' status.");

                // Update status to "No Translation Needed"
                publicationRequest.setStatus(PublicationRequestStatus.NO_TRANSLATION_NEEDED.getId());
                publicationRequestDAO.save(context, publicationRequest);

                log.info("Updated PublicationRequest ID: " + publicationRequest.getId()
                        + " status to 'No Translation Needed'");
                return;
            }

            log.info("Step 3");

            List<Bundle> originalBundles = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);

            if (originalBundles == null || originalBundles.isEmpty()) {
                log.info("No ORIGINAL bundle found for item: " + itemUuid
                        + ", PublicationRequest ID: " + publicationRequest.getId());
                return;
            }

            log.info("Step 4");

            int translationRequestCount = 0;
            int existingTranslationRequestCount = 0;

            // Iterate through each ORIGINAL bundle and its bitstreams
            for (Bundle bundle : originalBundles) {
                List<Bitstream> bitstreams = bundle.getBitstreams();

                if (bitstreams != null && !bitstreams.isEmpty()) {
                    for (Bitstream bitstream : bitstreams) {
                        String bitstreamUUID = bitstream.getID().toString();

                        // Check if a TranslationRequest already exists for this bitstream and publication
                        TranslationRequest existingTranslationRequest =
                            translationRequestService.findByBitstreamUUIDAndPublicationUUID(
                                context, bitstreamUUID, publicationRequest.getPublicationUUID());

                        log.info("Step 5");

                        TranslationRequest translationRequest;

                        if (existingTranslationRequest != null) {
                            log.info("Step 6");
                            // TranslationRequest already exists - reuse it
                            translationRequest = existingTranslationRequest;
                            existingTranslationRequestCount++;
                            log.info("Found existing TranslationRequest ID: " + translationRequest.getId()
                                    + " for Bitstream: " + bitstreamUUID
                                    + ", reusing for PublicationRequest ID: " + publicationRequest.getId());
                        } else {
                            log.info("Step 7");
                            // Create a new translation request for this bitstream
                            translationRequest = translationRequestService.create(context);
                            translationRequest.setPublicationUUID(publicationRequest.getPublicationUUID());
                            translationRequest.setBitstreamUUID(bitstreamUUID);
                            translationRequest.setLanguage(publicationRequest.getLanguage());
                            // Set initial status to "New"
                            translationRequest.setStatus(
                                org.dspace.translationrequest.TranslationRequestStatus.NEW.getId()
                            );
                            translationRequest.setCreatedDate(new Date());

                            // Save the translation request
                            translationRequestService.updateWithoutAuthCheck(context, translationRequest);
                            translationRequestCount++;

                            log.info("Created TranslationRequest ID: " + translationRequest.getId()
                                    + " for Bitstream: " + bitstreamUUID
                                    + " linked to PublicationRequest ID: " + publicationRequest.getId());
                        }

                        // Create Translation2Publication link entity (whether new or existing TranslationRequest)
                        createTranslation2PublicationLink(context, translationRequest.getId(),
                                                         publicationRequest.getId());
                    }
                }
            }

            log.info("Created " + translationRequestCount + " new TranslationRequest(s) and reused "
                    + existingTranslationRequestCount + " existing TranslationRequest(s) for PublicationRequest ID: "
                    + publicationRequest.getId());

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for publicationUUID: " + publicationRequest.getPublicationUUID(), e);
        } catch (SQLException | AuthorizeException e) {
            log.error("Error creating translation requests for PublicationRequest ID: "
                    + publicationRequest.getId(), e);
        } catch (Exception e) {
            log.error("Error creating translation requests for PublicationRequest ID: "
                    + publicationRequest.getId(), e);
            throw e;
        }
    }

    /**
     * Create a Translation2Publication link entity between a translation request and publication request
     *
     * @param context The DSpace context
     * @param translationRequestId The ID of the translation request
     * @param publicationRequestId The ID of the publication request
     */
    private void createTranslation2PublicationLink(Context context, Integer translationRequestId,
                                                    Integer publicationRequestId) {
        try {
            Translation2Publication link = translation2PublicationService.create(context);
            link.setTranslationRequestId(translationRequestId);
            link.setPublicationRequestId(publicationRequestId);

            translation2PublicationService.update(context, link);

            log.info("Created Translation2Publication link: TranslationRequest ID " + translationRequestId
                    + " <-> PublicationRequest ID " + publicationRequestId);

        } catch (SQLException | AuthorizeException e) {
            log.error("Error creating Translation2Publication link for TranslationRequest ID: "
                    + translationRequestId + " and PublicationRequest ID: " + publicationRequestId, e);
        }
    }

    @Override
    public void delete(Context context, PublicationRequest publicationRequest)
        throws SQLException, AuthorizeException {
        // Only administrators can delete
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only administrators can delete publication requests");
        }
        publicationRequestDAO.delete(context, publicationRequest);
        log.info("Deleted PublicationRequest with ID: " + publicationRequest.getId());
    }

    @Override
    public List<PublicationRequest> findByPublicationUUID(Context context, String publicationUUID)
        throws SQLException {
        return publicationRequestDAO.findByPublicationUUID(context, publicationUUID);
    }

    @Override
    public List<PublicationRequest> findByUserEmailAddress(Context context, String userEmailAddress)
        throws SQLException {
        return publicationRequestDAO.findByUserEmailAddress(context, userEmailAddress);
    }

    @Override
    public List<PublicationRequest> findAll(Context context, int offset, int limit) throws SQLException {
        return publicationRequestDAO.findAll(context, offset, limit);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return publicationRequestDAO.countRows(context);
    }

    @Override
    public List<PublicationRequest> findByTitle(Context context, String title, int offset, int limit)
        throws SQLException {
        return publicationRequestDAO.findByTitle(context, title, offset, limit);
    }

    @Override
    public int countByTitle(Context context, String title) throws SQLException {
        return publicationRequestDAO.countByTitle(context, title);
    }

    @Override
    public List<PublicationRequest> findByTranslationRequestId(Context context, Integer translationRequestId,
                                                                 int offset, int limit) throws SQLException {
        return publicationRequestDAO.findByTranslationRequestId(context, translationRequestId, offset, limit);
    }

    @Override
    public int countByTranslationRequestId(Context context, Integer translationRequestId) throws SQLException {
        return publicationRequestDAO.countByTranslationRequestId(context, translationRequestId);
    }

    @Override
    public List<PublicationRequest> findByStatus(Context context, int status, int offset, int limit)
        throws SQLException {
        return publicationRequestDAO.findByStatus(context, status, offset, limit);
    }

    @Override
    public int countByStatus(Context context, int status) throws SQLException {
        return publicationRequestDAO.countByStatus(context, status);
    }

    /**
     * Check if the publication is already available in the requested language
     * by examining isLanguageOfPublication relationships
     *
     * @param context The DSpace context
     * @param item The item to check
     * @param requestedLanguage The requested language ('en' or 'fr')
     * @return true if the publication is already available in the requested language
     */
    private boolean isPublicationAvailableInLanguage(Context context, Item item, String requestedLanguage) {
        try {
            log.info("Checking Language");

            // Find the relationship type "isLanguageOfPublication"
            List<RelationshipType> relationshipTypes = relationshipTypeService
                .findByLeftwardOrRightwardTypeName(context, "isLanguageOfPublication");

            if (relationshipTypes == null || relationshipTypes.isEmpty()) {
                log.info("No 'isLanguageOfPublication' relationship type found");
                return false;
            }

            // Check relationships for each relationship type (there should typically be only one)
            for (RelationshipType relationshipType : relationshipTypes) {

                log.info("Checking Rels for Type: " + relationshipType.getID());

                List<Relationship> relationships = relationshipService
                    .findByItemAndRelationshipType(context, item, relationshipType);

                if (relationships != null && !relationships.isEmpty()) {
                    for (Relationship relationship : relationships) {
                        // Get the related item (the language item)
                        Item relatedItem = relationship.getLeftItem().equals(item)
                            ? relationship.getRightItem()
                            : relationship.getLeftItem();

                        // Check if the related item has the ISO code matching the requested language
                        // The language ISO code should be in dc.identifier.iso
                        String isoCode = itemService.getMetadataFirstValue(
                            relatedItem, "dc", "identifier", "iso", Item.ANY);

                        if (isoCode != null && isoCode.equalsIgnoreCase(requestedLanguage)) {
                            log.info("Found matching language relationship: Item " + item.getID()
                                    + " has isLanguageOfPublication relationship with language item "
                                    + relatedItem.getID() + " (ISO code: " + isoCode + ")");
                            return true;
                        }
                    }
                }
            }

            log.info("No matching language relationship found for item " + item.getID()
                    + " and language: " + requestedLanguage);
            return false;

        } catch (SQLException e) {
            log.error("Error checking language availability for item " + item.getID(), e);
            return false;
        }
    }
}
