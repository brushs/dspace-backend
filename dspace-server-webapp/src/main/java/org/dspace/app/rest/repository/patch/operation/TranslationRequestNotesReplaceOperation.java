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
import org.dspace.translationrequest.TranslationRequest;
import org.springframework.stereotype.Component;

/**
 * Implementation for TranslationRequest notes replace patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/request/translationrequests/<:id> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "/notes", "value": "Updated notes"}]'
 * </code>
 *
 * @author [Your Name]
 */
@Component
public class TranslationRequestNotesReplaceOperation<R> extends PatchOperation<R> {

    private static final Logger log = LogManager.getLogger();

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_NOTES = "/notes";

    @Override
    public R perform(Context context, R object, Operation operation) {
        if (supports(object, operation)) {
            TranslationRequest translationRequest = (TranslationRequest) object;

            // Notes can be null (to clear the field) or a string
            String notes = null;
            if (operation.getValue() != null) {
                if (operation.getValue() instanceof String) {
                    notes = (String) operation.getValue();
                } else {
                    notes = operation.getValue().toString();
                }
            }

            log.info("Updating TranslationRequest ID {} notes", translationRequest.getId());

            translationRequest.setNotes(notes);
            return object;
        } else {
            throw new DSpaceBadRequestException(
                "TranslationRequestNotesReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof TranslationRequest &&
                operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE) &&
                operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_NOTES));
    }
}
