/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.*;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Execute three validation check on fields validation:
 * - mandatory metadata missing
 * - regex missing match
 * - authority required metadata missing
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class CustomValidationTwo extends AbstractValidation {

    private static final String ERROR_VALIDATION_REQUIRED = "error.validation.required";

    private static final String ERROR_VALIDATION_AUTHORITY_REQUIRED = "error.validation.authority.required";

    private static final String ERROR_VALIDATION_REGEX = "error.validation.regex";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CustomValidationTwo.class);

    private DCInputsReader inputReader;

    private ItemService itemService;

    private MetadataAuthorityService metadataAuthorityService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {

        log.error("IN VALIDATION STEP TWO");
        if ("traditionalpageone".equals(config.getId())) {
            return getErrors();
        }
        String fieldName = "dc.contributor.author";
        List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), fieldName);
        boolean found = false;
        boolean exists = false;
        for (MetadataValue md : mdv) {
            exists = true;
            if ("Wayne".equals(md.getValue())) {
                found = true;
            }
        }

        if (exists && !found) {
            addError(ERROR_VALIDATION_AUTHORITY_REQUIRED,
                    "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() +
                            "/" + fieldName);
        }

        return getErrors();
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setMetadataAuthorityService(MetadataAuthorityService metadataAuthorityService) {
        this.metadataAuthorityService = metadataAuthorityService;
    }

    public DCInputsReader getInputReader() {
        if (inputReader == null) {
            try {
                inputReader = new DCInputsReader();
            } catch (DCInputsReaderException e) {
                log.error(e.getMessage(), e);
            }
        }
        return inputReader;
    }

    public void setInputReader(DCInputsReader inputReader) {
        this.inputReader = inputReader;
    }

}