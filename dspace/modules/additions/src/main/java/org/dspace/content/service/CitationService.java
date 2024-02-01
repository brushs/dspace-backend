package org.dspace.content.service;

import org.dspace.content.MetadataValue;
import org.dspace.content.Term;
import org.dspace.content.Vocabulary;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface CitationService {

    MetadataValue getCitation(List<MetadataValue> dbValues);

}
