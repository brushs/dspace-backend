/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.Translation2PublicationConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.Translation2PublicationRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.translation2publication.Translation2Publication;
import org.dspace.translation2publication.Translation2PublicationId;
import org.dspace.translation2publication.service.Translation2PublicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Translation2Publication Rest object
 *
 * @author [Your Name]
 */
@Component(Translation2PublicationRest.CATEGORY + "." + Translation2PublicationRest.NAME)
public class Translation2PublicationRestRepository extends DSpaceRestRepository<Translation2PublicationRest, String> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private Translation2PublicationService translation2PublicationService;

    @Autowired
    private Translation2PublicationConverter converter;

    public Translation2PublicationRestRepository() {
        super();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Translation2PublicationRest findOne(Context context, String id) {
        try {
            // Parse composite ID from string format: "translationRequestId_publicationRequestId"
            String[] parts = id.split("_");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                    "Invalid ID format. Expected: translationRequestId_publicationRequestId");
            }
            Integer translationRequestId = Integer.parseInt(parts[0]);
            Integer publicationRequestId = Integer.parseInt(parts[1]);

            Translation2PublicationId compositeId = new Translation2PublicationId(translationRequestId,
                publicationRequestId);
            Translation2Publication translation2Publication = translation2PublicationService.find(context, compositeId);
            if (translation2Publication == null) {
                return null;
            }
            return converter.convert(translation2Publication, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Error finding Translation2Publication with id: " + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<Translation2PublicationRest> findAll(Context context, Pageable pageable) {
        try {
            int total = translation2PublicationService.countTotal(context);
            List<Translation2Publication> translation2Publications = translation2PublicationService.findAll(
                context,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<Translation2PublicationRest> restList = translation2Publications.stream()
                .map(t2p -> converter.convert(t2p, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException e) {
            log.error("Error finding all Translation2Publications", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("permitAll()")
    protected Translation2PublicationRest createAndReturn(Context context) throws AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        Translation2PublicationRest requestRest;
        try {
            requestRest = mapper.readValue(req.getInputStream(), Translation2PublicationRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing the request body", e);
        }

        // Validate required fields
        if (requestRest.getTranslationRequestId() == null) {
            throw new UnprocessableEntityException("translationRequestId is required");
        }
        if (requestRest.getPublicationRequestId() == null) {
            throw new UnprocessableEntityException("publicationRequestId is required");
        }

        Translation2Publication translation2Publication;
        try {
            // Create the entity and set values
            translation2Publication = translation2PublicationService.create(context);
            translation2Publication.setTranslationRequestId(requestRest.getTranslationRequestId());
            translation2Publication.setPublicationRequestId(requestRest.getPublicationRequestId());
            translation2PublicationService.update(context, translation2Publication);
        } catch (SQLException e) {
            log.error("Error creating Translation2Publication", e);
            throw new RuntimeException("Error creating Translation2Publication", e);
        }

        return converter.convert(translation2Publication, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, String id) throws AuthorizeException {
        try {
            // Parse composite ID from string format: "translationRequestId_publicationRequestId"
            String[] parts = id.split("_");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                    "Invalid ID format. Expected: translationRequestId_publicationRequestId");
            }
            Integer translationRequestId = Integer.parseInt(parts[0]);
            Integer publicationRequestId = Integer.parseInt(parts[1]);

            Translation2PublicationId compositeId = new Translation2PublicationId(translationRequestId,
                publicationRequestId);
            Translation2Publication translation2Publication = translation2PublicationService.find(context, compositeId);
            if (translation2Publication != null) {
                translation2PublicationService.delete(context, translation2Publication);
            }
        } catch (SQLException e) {
            log.error("Error deleting Translation2Publication with id: " + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for translation2publication links by translation request ID
     *
     * @param translationRequestId The translation request ID to search for
     * @param pageable             Pagination information
     * @return Page of Translation2PublicationRest objects
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byTranslationRequestId")
    public Page<Translation2PublicationRest> findByTranslationRequestId(
        @Parameter(value = "translationRequestId", required = true) Integer translationRequestId,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            List<Translation2Publication> translation2Publications =
                translation2PublicationService.findByTranslationRequestId(context, translationRequestId);
            List<Translation2PublicationRest> restList = translation2Publications.stream()
                .map(t2p -> converter.convert(t2p, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, restList.size());
        } catch (SQLException e) {
            log.error("Error finding Translation2Publications by translation request ID: " + translationRequestId, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for translation2publication links by publication request ID
     *
     * @param publicationRequestId The publication request ID to search for
     * @param pageable             Pagination information
     * @return Page of Translation2PublicationRest objects
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byPublicationRequestId")
    public Page<Translation2PublicationRest> findByPublicationRequestId(
        @Parameter(value = "publicationRequestId", required = true) Integer publicationRequestId,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            List<Translation2Publication> translation2Publications =
                translation2PublicationService.findByPublicationRequestId(context, publicationRequestId);
            List<Translation2PublicationRest> restList = translation2Publications.stream()
                .map(t2p -> converter.convert(t2p, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, restList.size());
        } catch (SQLException e) {
            log.error("Error finding Translation2Publications by publication request ID: " + publicationRequestId, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<Translation2PublicationRest> getDomainClass() {
        return Translation2PublicationRest.class;
    }
}

