package org.dspace.content.service;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

import java.util.List;

public interface CitationService {

    MetadataValue getCitation(Item item, List<MetadataValue> dbValues);

}
