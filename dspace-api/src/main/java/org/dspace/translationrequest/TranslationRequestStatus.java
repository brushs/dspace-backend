/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translationrequest;

/**
 * Enum representing the possible states of a translation request.
 *
 * @author [Your Name]
 */
public enum TranslationRequestStatus {
    NEW(1, "New"),
    TRANSLATION_IN_PROGRESS(2, "Translation In Progress"),
    COMPLETED(3, "Completed"),
    CANCELLED(4, "Cancelled"),
    ON_HOLD(5, "On Hold");

    private final int id;
    private final String name;

    TranslationRequestStatus(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the status enum from an integer ID
     *
     * @param id The status ID
     * @return The TranslationRequestStatus enum, or null if not found
     */
    public static TranslationRequestStatus fromId(int id) {
        for (TranslationRequestStatus status : TranslationRequestStatus.values()) {
            if (status.id == id) {
                return status;
            }
        }
        return null;
    }

    /**
     * Get the status enum from a name string
     *
     * @param name The status name
     * @return The TranslationRequestStatus enum, or null if not found
     */
    public static TranslationRequestStatus fromName(String name) {
        for (TranslationRequestStatus status : TranslationRequestStatus.values()) {
            if (status.name.equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
