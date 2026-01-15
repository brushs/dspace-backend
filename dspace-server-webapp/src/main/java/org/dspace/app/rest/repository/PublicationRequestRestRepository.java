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
import org.dspace.app.rest.converter.PublicationRequestConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.PublicationRequestRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.publicationrequest.PublicationRequest;
import org.dspace.publicationrequest.service.PublicationRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage PublicationRequest Rest object
 *
 * @author [Your Name]
 */
@Component(PublicationRequestRest.CATEGORY + "." + PublicationRequestRest.NAME)
public class PublicationRequestRestRepository extends DSpaceRestRepository<PublicationRequestRest, Integer> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private PublicationRequestService publicationRequestService;

    @Autowired
    private PublicationRequestConverter converter;

    @Autowired
    private AuthorizeService authorizeService;

    public PublicationRequestRestRepository() {
        super();
    }

    @Override
    @PreAuthorize("permitAll()")
    public PublicationRequestRest findOne(Context context, Integer id) {
        try {
            // TODO: Re-enable admin check for production
            // Temporarily public for development
            // if (!authorizeService.isAdmin(context)) {
            //     throw new AuthorizeException("Only administrators can view publication requests");
            // }
            PublicationRequest publicationRequest = publicationRequestService.find(context, id);
            if (publicationRequest == null) {
                return null;
            }
            return converter.convert(publicationRequest, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Error finding PublicationRequest with id: " + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<PublicationRequestRest> findAll(Context context, Pageable pageable) {
        try {
            // Only admins can list all publication requests
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException("Only administrators can list publication requests");
            }
            int total = publicationRequestService.countTotal(context);
            List<PublicationRequest> publicationRequests = publicationRequestService.findAll(
                context,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<PublicationRequestRest> restList = publicationRequests.stream()
                .map(pr -> converter.convert(pr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException | AuthorizeException e) {
            log.error("Error finding all PublicationRequests", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected PublicationRequestRest createAndReturn(Context context) throws AuthorizeException {
        // Creation is public - no admin check required
        // CSRF protection is automatically handled by Spring Security for POST requests from the UI
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        PublicationRequestRest requestRest;
        try {
            requestRest = mapper.readValue(req.getInputStream(), PublicationRequestRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing the request body", e);
        }

        // Validate required fields
        if (requestRest.getPublicationGUID() == null || requestRest.getPublicationGUID().isEmpty()) {
            throw new UnprocessableEntityException("publicationGUID is required");
        }
        if (requestRest.getUserEmailAddress() == null || requestRest.getUserEmailAddress().isEmpty()) {
            throw new UnprocessableEntityException("userEmailAddress is required");
        }
        if (requestRest.getLanguage() == null || requestRest.getLanguage().isEmpty()) {
            throw new UnprocessableEntityException("language is required");
        }

        PublicationRequest publicationRequest;
        try {
            // Create the entity and set values directly (bypass update authorization)
            publicationRequest = publicationRequestService.create(context);
            publicationRequest.setPublicationGUID(requestRest.getPublicationGUID());
            publicationRequest.setUserEmailAddress(requestRest.getUserEmailAddress());
            publicationRequest.setLanguage(requestRest.getLanguage());
            publicationRequest.setStatus(requestRest.getStatus());
            // Directly save without going through update() which requires admin
            context.turnOffAuthorisationSystem();
            publicationRequestService.updateWithoutAuthCheck(context, publicationRequest);
            context.restoreAuthSystemState();
        } catch (SQLException e) {
            log.error("Error creating PublicationRequest", e);
            throw new RuntimeException("Error creating PublicationRequest", e);
        }

        return converter.convert(publicationRequest, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            PublicationRequest publicationRequest = publicationRequestService.find(context, id);
            if (publicationRequest != null) {
                publicationRequestService.delete(context, publicationRequest);
            }
        } catch (SQLException e) {
            log.error("Error deleting PublicationRequest with id: " + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for publication requests by publication GUID
     *
     * @param guid     The publication GUID to search for
     * @param pageable Pagination information
     * @return Page of PublicationRequestRest objects
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byPublicationGUID")
    public Page<PublicationRequestRest> findByPublicationGUID(
        @Parameter(value = "guid", required = true) String guid,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            List<PublicationRequest> publicationRequests =
                publicationRequestService.findByPublicationGUID(context, guid);
            List<PublicationRequestRest> restList = publicationRequests.stream()
                .map(pr -> converter.convert(pr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, restList.size());
        } catch (SQLException e) {
            log.error("Error finding PublicationRequests by publication GUID: " + guid, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for publication requests by user email address
     *
     * @param email    The email address to search for
     * @param pageable Pagination information
     * @return Page of PublicationRequestRest objects
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byUserEmail")
    public Page<PublicationRequestRest> findByUserEmail(
        @Parameter(value = "email", required = true) String email,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            List<PublicationRequest> publicationRequests =
                publicationRequestService.findByUserEmailAddress(context, email);
            List<PublicationRequestRest> restList = publicationRequests.stream()
                .map(pr -> converter.convert(pr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, restList.size());
        } catch (SQLException e) {
            log.error("Error finding PublicationRequests by user email: " + email, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for publication requests by title (searches both English and French titles)
     *
     * @param title    The title to search for (case-insensitive partial match)
     * @param pageable Pagination information
     * @return Page of PublicationRequestRest objects
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byTitle")
    public Page<PublicationRequestRest> findByTitle(
        @Parameter(value = "title", required = true) String title,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            int total = publicationRequestService.countByTitle(context, title);
            List<PublicationRequest> publicationRequests = publicationRequestService.findByTitle(
                context,
                title,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<PublicationRequestRest> restList = publicationRequests.stream()
                .map(pr -> converter.convert(pr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException e) {
            log.error("Error finding PublicationRequests by title: " + title, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for publication requests by translation request ID
     *
     * @param translationRequestId The translation request ID to search for
     * @param pageable             Pagination information
     * @return Page of PublicationRequestRest objects
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byTranslationRequestId")
    public Page<PublicationRequestRest> findByTranslationRequestId(
        @Parameter(value = "translationRequestId", required = true) Integer translationRequestId,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            int total = publicationRequestService.countByTranslationRequestId(context, translationRequestId);
            List<PublicationRequest> publicationRequests = publicationRequestService.findByTranslationRequestId(
                context,
                translationRequestId,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<PublicationRequestRest> restList = publicationRequests.stream()
                .map(pr -> converter.convert(pr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException e) {
            log.error("Error finding PublicationRequests by translation request ID: " + translationRequestId, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<PublicationRequestRest> getDomainClass() {
        return PublicationRequestRest.class;
    }
}

