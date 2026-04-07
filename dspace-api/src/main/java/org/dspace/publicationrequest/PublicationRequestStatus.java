/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest;

/**
 * Enum representing the possible states of a publication request.
 *
 * @author [Your Name]
 */
public enum PublicationRequestStatus {
    PENDING_TRANSLATION(1, "Pending Translation"),
    TRANSLATION_IN_PROGRESS(2, "Translation In Progress"),
    PENDING_NOTIFICATION(3, "Pending Notification"),
    COMPLETED(4, "Completed"),
    CANCELLED(5, "Cancelled"),
    ON_HOLD(6, "On Hold"),
    ERROR(7, "Error"),
    NO_TRANSLATION_NEEDED(8, "No Translation Needed");

    private final int id;
    private final String name;

    PublicationRequestStatus(int id, String name) {
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
     * @return The PublicationRequestStatus enum, or null if not found
     */
    public static PublicationRequestStatus fromId(int id) {
        for (PublicationRequestStatus status : PublicationRequestStatus.values()) {
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
     * @return The PublicationRequestStatus enum, or null if not found
     */
    public static PublicationRequestStatus fromName(String name) {
        for (PublicationRequestStatus status : PublicationRequestStatus.values()) {
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
