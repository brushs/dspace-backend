/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.TranslationRequestRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.translationrequest.TranslationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the TranslationRequest in the DSpace API data model and the
 * REST data model
 *
 * @author [Your Name]
 */
@Component
public class TranslationRequestConverter implements DSpaceConverter<TranslationRequest, TranslationRequestRest> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Override
    public TranslationRequestRest convert(TranslationRequest obj, Projection projection) {
        TranslationRequestRest rest = new TranslationRequestRest();
        rest.setProjection(projection);
        rest.setId(obj.getId());
        rest.setPublicationUUID(obj.getPublicationUUID());
        rest.setBitstreamUUID(obj.getBitstreamUUID());
        rest.setLanguage(obj.getLanguage());
        rest.setStatus(obj.getStatus());
        rest.setCreatedDate(obj.getCreatedDate());
        rest.setClosedDate(obj.getClosedDate());

        // Fetch title metadata from the Item if publicationUUID is a valid UUID
        if (obj.getPublicationUUID() != null) {
            try {
                UUID itemUuid = UUID.fromString(obj.getPublicationUUID());
                Context context = ContextUtil.obtainCurrentRequestContext();
                if (context != null) {
                    Item item = itemService.find(context, itemUuid);
                    if (item != null) {
                        // Get English title (dc.title with language 'en' or no language)
                        List<MetadataValue> titleMetadata = itemService.getMetadata(
                            item, "dc", "title", null, "en"
                        );
                        if (titleMetadata != null && !titleMetadata.isEmpty()) {
                            rest.setTitleEn(titleMetadata.get(0).getValue());
                        } else {
                            // If no 'en' title, try without language specification
                            titleMetadata = itemService.getMetadata(
                                item, "dc", "title", null, Item.ANY
                            );
                            if (titleMetadata != null && !titleMetadata.isEmpty()) {
                                rest.setTitleEn(titleMetadata.get(0).getValue());
                            }
                        }

                        // Get French title (dc.title with language 'fr')
                        List<MetadataValue> titleMetadataFr = itemService.getMetadata(
                            item, "dc", "title", null, "fr"
                        );
                        if (titleMetadataFr != null && !titleMetadataFr.isEmpty()) {
                            rest.setTitleFr(titleMetadataFr.get(0).getValue());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // publicationUUID is not a valid UUID, skip title lookup
                log.debug("publicationUUID is not a valid UUID: " + obj.getPublicationUUID());
            } catch (SQLException e) {
                log.error("Error fetching item for publicationUUID: " + obj.getPublicationUUID(), e);
            }
        }

        // Fetch bitstream name from the Bitstream if bitstreamUUID is a valid UUID
        if (obj.getBitstreamUUID() != null) {
            try {
                UUID bitstreamUuid = UUID.fromString(obj.getBitstreamUUID());
                Context context = ContextUtil.obtainCurrentRequestContext();
                if (context != null) {
                    Bitstream bitstream = bitstreamService.find(context, bitstreamUuid);
                    if (bitstream != null && bitstream.getInternalId() != null) {
                        // Extract filename from internal_id (part after last "/")
                        String internalId = bitstream.getInternalId();
                        int lastSlashIndex = internalId.lastIndexOf('/');
                        if (lastSlashIndex >= 0 && lastSlashIndex < internalId.length() - 1) {
                            rest.setBitstreamName(internalId.substring(lastSlashIndex + 1));
                        } else {
                            // If no "/" found, use the whole internal_id
                            rest.setBitstreamName(internalId);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // bitstreamUUID is not a valid UUID, skip bitstream lookup
                log.debug("bitstreamUUID is not a valid UUID: " + obj.getBitstreamUUID());
            } catch (SQLException e) {
                log.error("Error fetching bitstream for bitstreamUUID: " + obj.getBitstreamUUID(), e);
            }
        }

        return rest;
    }

    @Override
    public Class<TranslationRequest> getModelClass() {
        return TranslationRequest.class;
    }
}

