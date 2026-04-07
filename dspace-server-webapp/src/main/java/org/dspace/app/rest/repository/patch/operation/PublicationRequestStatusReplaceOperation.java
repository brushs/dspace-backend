/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.publicationrequest.PublicationRequest;
import org.dspace.publicationrequest.PublicationRequestStatus;
import org.springframework.stereotype.Component;

/**
 * Implementation for PublicationRequest status replace patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/request/publicationrequests/<:id> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "/status", "value": "4"}]'
 * </code>
 *
 * @author [Your Name]
 */
@Component
public class PublicationRequestStatusReplaceOperation<R> extends PatchOperation<R> {

    private static final Logger log = LogManager.getLogger();

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_STATUS = "/status";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            PublicationRequest publicationRequest = (PublicationRequest) object;

            // Parse the status value - can be either a status ID (integer) or status name (string)
            Integer statusId = null;
            Object value = operation.getValue();

            if (value instanceof Integer) {
                statusId = (Integer) value;
            } else if (value instanceof String) {
                String statusValue = (String) value;
                // Try to parse as status name first
                PublicationRequestStatus statusEnum = PublicationRequestStatus.fromName(statusValue);
                if (statusEnum != null) {
                    statusId = statusEnum.getId();
                } else {
                    // Try to parse as integer
                    try {
                        statusId = Integer.parseInt(statusValue);
                    } catch (NumberFormatException e) {
                        throw new DSpaceBadRequestException("Invalid status value: " + statusValue +
                            ". Must be a valid status ID or name.");
                    }
                }
            }

            if (statusId == null) {
                throw new DSpaceBadRequestException("Status value is required");
            }

            // Validate that the status ID is valid
            PublicationRequestStatus status = PublicationRequestStatus.fromId(statusId);
            if (status == null) {
                throw new DSpaceBadRequestException("Invalid status ID: " + statusId);
            }

            log.info("Updating PublicationRequest ID {} status to {} ({})",
                publicationRequest.getId(), status.getName(), statusId);

            publicationRequest.setStatus(statusId);
            return object;
        } else {
            throw new DSpaceBadRequestException(
                "PublicationRequestStatusReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof PublicationRequest &&
                operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
                operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_STATUS));
    }
}
