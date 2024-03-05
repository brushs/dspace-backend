package org.dspace.discovery.utils;

import org.dspace.content.MetadataValue;

import java.util.List;

public class IndexingUtil {

    public static boolean processRelationshipsForItem(List<MetadataValue> values) {
        if (values == null || values.size() == 0) {
            return true;
        }

        if (values.get(0).getValue().contentEquals("Language") ||
                values.get(0).getValue().contentEquals("Province")) {
            return false;
        }

        return true;
    }
}
