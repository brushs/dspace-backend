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
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.TranslationRequestConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.TranslationRequestRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.translationrequest.TranslationRequest;
import org.dspace.translationrequest.service.TranslationRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage TranslationRequest Rest object
 *
 * @author [Your Name]
 */
@Component(TranslationRequestRest.CATEGORY + "." + TranslationRequestRest.NAME)
public class TranslationRequestRestRepository extends DSpaceRestRepository<TranslationRequestRest, Integer> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private TranslationRequestService translationRequestService;

    @Autowired
    private TranslationRequestConverter converter;

    @Autowired
    private AuthorizeService authorizeService;

    public TranslationRequestRestRepository() {
        super();
    }

    @Override
    @PreAuthorize("permitAll()")
    public TranslationRequestRest findOne(Context context, Integer id) {
        try {
            // TODO: Re-enable admin check for production
            // Temporarily public for development
            // if (!authorizeService.isAdmin(context)) {
            //     throw new AuthorizeException("Only administrators can view translation requests");
            // }
            TranslationRequest translationRequest = translationRequestService.find(context, id);
            if (translationRequest == null) {
                return null;
            }
            return converter.convert(translationRequest, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Error finding TranslationRequest with id: " + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<TranslationRequestRest> findAll(Context context, Pageable pageable) {
        try {
            // Only admins can list all translation requests
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException("Only administrators can list translation requests");
            }
            int total = translationRequestService.countTotal(context);
            List<TranslationRequest> translationRequests = translationRequestService.findAll(
                context,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<TranslationRequestRest> restList = translationRequests.stream()
                .map(tr -> converter.convert(tr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException | AuthorizeException e) {
            log.error("Error finding all TranslationRequests", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected TranslationRequestRest createAndReturn(Context context) throws AuthorizeException {
        // Creation is public - no admin check required
        // CSRF protection is automatically handled by Spring Security for POST requests from the UI
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        TranslationRequestRest requestRest;
        try {
            requestRest = mapper.readValue(req.getInputStream(), TranslationRequestRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing the request body", e);
        }

        // Validate required fields
        if (requestRest.getPublicationUUID() == null || requestRest.getPublicationUUID().isEmpty()) {
            throw new UnprocessableEntityException("publicationUUID is required");
        }
        if (requestRest.getBitstreamUUID() == null || requestRest.getBitstreamUUID().isEmpty()) {
            throw new UnprocessableEntityException("bitstreamUUID is required");
        }
        if (requestRest.getLanguage() == null || requestRest.getLanguage().isEmpty()) {
            throw new UnprocessableEntityException("language is required");
        }

        TranslationRequest translationRequest;
        try {
            // Create the entity and set values directly (bypass update authorization)
            translationRequest = translationRequestService.create(context);
            translationRequest.setPublicationUUID(requestRest.getPublicationUUID());
            translationRequest.setBitstreamUUID(requestRest.getBitstreamUUID());
            translationRequest.setLanguage(requestRest.getLanguage());
            translationRequest.setStatus(requestRest.getStatus());
            // Set created date to current time if not provided
            if (requestRest.getCreatedDate() != null) {
                translationRequest.setCreatedDate(requestRest.getCreatedDate());
            } else {
                translationRequest.setCreatedDate(new Date());
            }
            // Set closed date if provided (nullable)
            if (requestRest.getClosedDate() != null) {
                translationRequest.setClosedDate(requestRest.getClosedDate());
            }
            // Set notes if provided (nullable)
            if (requestRest.getNotes() != null) {
                translationRequest.setNotes(requestRest.getNotes());
            }
            // Directly save without going through update() which requires admin
            context.turnOffAuthorisationSystem();
            translationRequestService.updateWithoutAuthCheck(context, translationRequest);
            context.restoreAuthSystemState();
        } catch (SQLException e) {
            log.error("Error creating TranslationRequest", e);
            throw new RuntimeException("Error creating TranslationRequest", e);
        }

        return converter.convert(translationRequest, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            TranslationRequest translationRequest = translationRequestService.find(context, id);
            if (translationRequest != null) {
                translationRequestService.delete(context, translationRequest);
            }
        } catch (SQLException e) {
            log.error("Error deleting TranslationRequest with id: " + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for translation requests by publication UUID
     *
     * @param uuid     The publication UUID to search for
     * @param pageable Pagination information
     * @return Page of TranslationRequestRest objects
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byPublicationUuid")
    public Page<TranslationRequestRest> findByPublicationUuid(
        @Parameter(value = "uuid", required = true) String uuid,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            List<TranslationRequest> translationRequests =
                translationRequestService.findByPublicationUUID(context, uuid);
            List<TranslationRequestRest> restList = translationRequests.stream()
                .map(tr -> converter.convert(tr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, restList.size());
        } catch (SQLException e) {
            log.error("Error finding TranslationRequests by publication UUID: " + uuid, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<TranslationRequestRest> getDomainClass() {
        return TranslationRequestRest.class;
    }

    /**
     * Search for translation requests by publication title (searches both English and French titles)
     *
     * @param title    The title to search for (case-insensitive partial match)
     * @param pageable Pagination information
     * @return Page of TranslationRequestRest objects
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byTitle")
    public Page<TranslationRequestRest> findByTitle(
        @Parameter(value = "title", required = true) String title,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            int total = translationRequestService.countByPublicationTitle(context, title);
            List<TranslationRequest> translationRequests = translationRequestService.findByPublicationTitle(
                context,
                title,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<TranslationRequestRest> restList = translationRequests.stream()
                .map(tr -> converter.convert(tr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException e) {
            log.error("Error finding TranslationRequests by title: " + title, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for translation requests by user email (from related publication request)
     *
     * @param email    The email address to search for
     * @param pageable Pagination information
     * @return Page of TranslationRequestRest objects
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byUserEmail")
    public Page<TranslationRequestRest> findByUserEmail(
        @Parameter(value = "email", required = true) String email,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            int total = translationRequestService.countByUserEmail(context, email);
            List<TranslationRequest> translationRequests = translationRequestService.findByUserEmail(
                context,
                email,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<TranslationRequestRest> restList = translationRequests.stream()
                .map(tr -> converter.convert(tr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException e) {
            log.error("Error finding TranslationRequests by user email: " + email, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Search for translation requests by status
     *
     * @param status   The status to search for
     * @param pageable Pagination information
     * @return Page of TranslationRequestRest objects
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byStatus")
    public Page<TranslationRequestRest> findByStatus(
        @Parameter(value = "status", required = true) int status,
        Pageable pageable
    ) {
        try {
            Context context = obtainContext();
            int total = translationRequestService.countByStatus(context, status);
            List<TranslationRequest> translationRequests = translationRequestService.findByStatus(
                context,
                status,
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize()
            );
            List<TranslationRequestRest> restList = translationRequests.stream()
                .map(tr -> converter.convert(tr, utils.obtainProjection()))
                .collect(java.util.stream.Collectors.toList());
            return new PageImpl<>(restList, pageable, total);
        } catch (SQLException e) {
            log.error("Error finding TranslationRequests by status: " + status, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

