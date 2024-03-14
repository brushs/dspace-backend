package org.dspace.metadata.util;

import org.apache.solr.common.StringUtils;
import org.dspace.content.MetadataValue;

import java.util.List;
import java.util.stream.Collectors;

public class MetadataUtils {

    public static List<MetadataValue> getFilteredList(List<MetadataValue> mdvs, String field) {

        String[] splittedKey = field.split("\\.");
        String schema = splittedKey.length > 0 ? splittedKey[0] : null;
        String element = splittedKey.length > 1 ? splittedKey[1] : null;
        String qualifier = splittedKey.length > 2 ? splittedKey[2] : null;

        List<MetadataValue> filteredMdvs = null;

        if (qualifier != null) {
            filteredMdvs =
                    mdvs.stream().filter(m -> m.getMetadataField().getMetadataSchema().getName().equals(schema)
                                    && m.getMetadataField().getElement().equals(element)
                                    && m.getMetadataField().getQualifier().equals(qualifier))
                            .collect(Collectors.toList());
        } else {
            filteredMdvs =
                    mdvs.stream().filter(m -> m.getMetadataField().getMetadataSchema().getName().equals(schema)
                                    && m.getMetadataField().getElement().equals(element)
                                    && StringUtils.isEmpty(m.getMetadataField().getQualifier()))
                            .collect(Collectors.toList());
        }

        return filteredMdvs;
    }
}
