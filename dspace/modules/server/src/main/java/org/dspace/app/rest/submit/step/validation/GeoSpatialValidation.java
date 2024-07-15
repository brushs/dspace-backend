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
import org.dspace.content.service.ItemService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Execute three validation check on fields validation:
 * - mandatory metadata missing
 * - regex missing match
 * - authority required metadata missing
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class GeoSpatialValidation extends AbstractValidation {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(GeoSpatialValidation.class);

    private static final String ERROR_VALIDATION_SINGLE_VALUE = "error.validation.singlevalue";
    private static final String ERROR_VALIDATION_INVALID_BBOX = "error.validation.invalidbbox";

    // Regular expression to match four floating point numbers separated by comma and space
    private static final String BOUNDING_BOX_REGEX = "^ENVELOPE\\(\\s*-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?\\)$";

    // Pattern object to compile the regex
    private static final Pattern pattern = Pattern.compile(BOUNDING_BOX_REGEX);

    private ItemService itemService;

    private List<ErrorRest> errors = new ArrayList<ErrorRest>();

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {

        log.info("Custom Validation - ensure only a single Bounding Box exists");
        if (!"geographicStep".equals(config.getId())) {
            return getErrors();
        }
        String fieldName = "geospatial.bbox";
        List<MetadataValue> mdvs = itemService.getMetadataByMetadataString(obj.getItem(), fieldName);
        if (mdvs == null || mdvs.isEmpty()) {
            return getErrors();
        }

        if (mdvs.size() > 1) {
            addError(ERROR_VALIDATION_SINGLE_VALUE,
                    "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() +
                            "/" + fieldName);
        }

        if (!isValidBoundingBox(mdvs.get(0).getValue())) {
            addError(ERROR_VALIDATION_INVALID_BBOX,
                    "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() +
                            "/" + fieldName);
        }

        return getErrors();
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    //Following are copy from the old class of AbstractValidation before 7.3. To keep the merged code working temprary
    /**
     * Add an error message (i18nKey) for a specific json path
     *
     * @param i18nKey
     *            the validation error message as a key to internationalize
     * @param path
     *            the json path that identify the wrong data in the submission. It could be as specific as a single
     *            value in a multivalued attribute or general of a "whole" section
     */
    public void addError(String i18nKey, String path) {
        boolean found = false;
        if (StringUtils.isNotBlank(i18nKey)) {
            for (ErrorRest error : errors) {
                if (i18nKey.equals(error.getMessage())) {
                    error.getPaths().add(path);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            ErrorRest error = new ErrorRest();
            error.setMessage(i18nKey);
            error.getPaths().add(path);
            errors.add(error);
        }
    }

    /**
     * Expose the identified errors
     *
     * @return the list of identified {@link ErrorRest}
     */
    public List<ErrorRest> getErrors() {
        return errors;
    }

    // Method to check if the string is a valid geospatial bounding box
    public boolean isValidBoundingBox(String boundingBox) {
        if (boundingBox == null || boundingBox.isEmpty()) {
            return false;
        }

        Matcher matcher = pattern.matcher(boundingBox);
        if (!matcher.matches()) {
            return false;
        }

        // Split the string into individual numbers
        boundingBox = boundingBox.substring(9, boundingBox.length() - 1);
        String[] parts = boundingBox.split(",");
        if (parts.length != 4) {
            return false;
        }

        try {
            double minLon = Double.parseDouble(parts[0]);
            double maxLon = Double.parseDouble(parts[1]);
            double maxLat = Double.parseDouble(parts[2]);
            double minLat = Double.parseDouble(parts[3]);

            // Additional checks to ensure valid geospatial coordinates
            if (minLon < -180 || minLon > 180 || maxLon < -180 || maxLon > 180) {
                return false;
            }

            if (minLat < -90 || minLat > 90 || maxLat < -90 || maxLat > 90) {
                return false;
            }

            if (minLon > maxLon || minLat > maxLat) {
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
