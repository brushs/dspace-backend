package org.dspace.content;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.MetadataLanguageSummaryDAO;
import org.dspace.content.dao.TermDAO;
import org.dspace.content.dao.VocabularyDAO;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VocabularyServiceImpl implements VocabularyService {

    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VocabularyServiceImpl.class);

    @Autowired(required = true)
    protected VocabularyDAO vocabularyDAO;

    @Autowired(required = true)
    protected TermDAO termDAO;

    @Autowired(required = true)
    protected MetadataLanguageSummaryDAO metadataLanguageSummaryDAO;

    @Autowired(required = true)
    protected ItemService itemService;

    @Override
    public Vocabulary findByName(Context context, String name)
            throws IOException, SQLException {

        return vocabularyDAO.findByName(context, name);
    }

    @Override
    public Vocabulary findById(Context context, int vocabularyId)
            throws IOException, SQLException {

        return vocabularyDAO.findByID(context, Vocabulary.class, vocabularyId);
    }

    @Override
    public List<Term> findByName(Context context, String termName, Integer vocabularyId)
            throws IOException, SQLException {

        return termDAO.findByName(context, termName, vocabularyId);
    }

    public List<Term> findByNameAndLang(Context context, String termName, Integer vocabularyId, String lang)
            throws IOException, SQLException {

        return termDAO.findByNameAndLang(context, termName, vocabularyId, lang);
    }

    @Override
    public List<Term> getRootTerms(Context context, int vocabularyId) throws IOException, SQLException {

        return termDAO.getRootTerms(context, vocabularyId);
    }

    @Override
    public List<Term> getChildTerms(Context context, int termId) throws IOException, SQLException {

        return termDAO.getChildTerms(context, termId);
    }

    @Override
    public List<MetadataLanguageSummary> getItemsForMetadataProcessingByType(Context context, int limit) throws IOException, SQLException {
        return metadataLanguageSummaryDAO.getItemsToProcessByType(context, limit);
    }

    @Override
    public List<MetadataLanguageSummary> getItemsForMetadataProcessingNoMPD(Context context, int limit) throws IOException, SQLException {
        return metadataLanguageSummaryDAO.getItemsToProcessNoMPD(context, limit);
    }

    @Override
    public List<MetadataLanguageSummary> getItemsForMetadataProcessingByMPD(Context context, int limit) throws IOException, SQLException {
        return metadataLanguageSummaryDAO.getItemsToProcessByMPD(context, limit);
    }

    /**
     * Shared 'perform' code between perform() and performItem() - a curation wrapper for the register() method
     * @param item the item
     */
    @Override
    public void performMetadataCopy(Context context, Item item) throws SQLException, IOException, AuthorizeException {
        // TODO pull from config
        Map<String, Integer> fieldsToProcess = new HashMap<String, Integer>();
        fieldsToProcess.put("dc.subject.cfs", 5);
        fieldsToProcess.put("dc.subject.gc", 2);
        fieldsToProcess.put("dc.subject.broad", 3);
        fieldsToProcess.put("dc.subject.geoscan", 1);
        fieldsToProcess.put("dc.subject.descriptor", 4);
        fieldsToProcess.put("dc.type", 6);

        for (String metadataField : fieldsToProcess.keySet()) {
            // Get Item metadata
            List<MetadataValue> mdvs = itemService.getMetadataByMetadataString(item, metadataField);

            Integer vocabularyId = fieldsToProcess.get(metadataField);

            List<String> langs = List.of("en", "fr");
            boolean found = false;
            for (MetadataValue mdv : mdvs) {
                log.info("Processing Value: " + mdv.getValue());
                // TODO Limit check to specific vocabulary based on config?
                // Check to see if any terms are matched
                for (String lang : langs) {
                    List<Term> terms = findByNameAndLang(context, mdv.getValue(), vocabularyId, lang);

                    if (!terms.isEmpty()) {
                        found = true;
                        Map<String, String> mappedMetadataFields = new HashMap<>();
                        mappedMetadataFields.put(metadataField + "_en", terms.get(0).getNameEn());
                        mappedMetadataFields.put(metadataField + "_fr", terms.get(0).getNameFr());

                        log.info("Found " + terms.size() + " Term(s) in " + lang);
                        for (Map.Entry<String, String> mappedMetadataField : mappedMetadataFields.entrySet()) {
                            // Check to see if mapped terms already exist
                            List<MetadataValue> mappedMdvs = itemService.getMetadataByMetadataString(item, mappedMetadataField.getKey());

                            boolean mappedValueExists = true;
                            if (mappedMdvs == null || mappedMdvs.size() == 0) {
                                mappedValueExists = false;
                            } else {
                                List<String> mdvValues = mappedMdvs.stream()
                                        .map(MetadataValue::getValue)
                                        .filter(x -> x != null)
                                        .collect(Collectors.toList());

                                mappedValueExists = mdvValues.stream().anyMatch(value -> value.equals(mappedMetadataField.getValue()));

                            }

                            if (!mappedValueExists) {
                                log.info("Adding new value");
                                // Copy to new metadata field
                                String[] tokens = mappedMetadataField.getKey().split("\\.");
                                itemService.addMetadata(context, item, tokens[0], tokens[1], tokens.length == 3 ? tokens[2] : null,
                                        mappedMetadataField.getKey().endsWith("_en") ? "en" : "fr", mappedMetadataField.getValue());
                                itemService.updateLastModified(context, item);
                            }
                        }
                    }
                }

                if (!found) {
                    // No matching text was found in Vocabulary
                    log.warn("Subject not found in Vocabulary - ID: " + item.getID() + " Val - " + mdv.getValue());
                }
            }
        }

        addOrUpdateMetadataCopiedDate(context, item);
    }

    private void addOrUpdateMetadataCopiedDate(Context context, Item item) throws SQLException, AuthorizeException {

        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, "nrcan.internal.metadataprocessdate");

        SimpleDateFormat fullIso2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (values.isEmpty()) {
            // add
            itemService.addMetadata(context, item, "nrcan", "internal", "metadataprocessdate", null, fullIso2.format(new Date()));
            log.info("Adding Metadata Copied Date for " + item.getID());
        } else {
            // update
            values.get(0).setValue(fullIso2.format(new Date()));
            log.info("Updating Metadata Copied Date for " + item.getID());
        }

        itemService.update(context, item);
    }

    @Override
    public void refreshMetadataSummaryMatView(Context context) {
        metadataLanguageSummaryDAO.refreshMaterializedView(context);
    }
}
