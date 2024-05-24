package org.dspace.app.rest.repository;

import java.util.*;

import org.dspace.app.rest.model.MetadataVocabularyRest;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.DCMetadataVocabulary;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(MetadataVocabularyRest.CATEGORY + "." + MetadataVocabularyRest.NAME)
public class MetadataVocabularyRepository extends DSpaceRestRepository<MetadataVocabularyRest, String> {
    private static final String[] METADATA_KEYS = {
            "BibliographicMetadata",
            "RecordManagement"
            // Add more keys here if needed
    };
    private Map<Locale, DCInputsReader> inputReaders;
    private DCInputsReader defaultInputReader;

    public MetadataVocabularyRepository() throws DCInputsReaderException {
        defaultInputReader = new DCInputsReader();
        Locale[] locales = I18nUtil.getSupportedLocales();
        inputReaders = new HashMap<Locale,DCInputsReader>();
        for (Locale locale : locales) {
            inputReaders.put(locale, new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
        }
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public MetadataVocabularyRest findOne(Context context, String metadataId) {
        Locale currentLocale = context.getCurrentLocale();
        DCInputsReader inputReader = inputReaders.getOrDefault(currentLocale, defaultInputReader);
        Map<String, DCMetadataVocabulary> formMetadataVocabulary = inputReader.GetMetadataVocabularyById(metadataId);

        // Iterate over the keys in order of relevance
        for (String key : METADATA_KEYS) {
            DCMetadataVocabulary entry = formMetadataVocabulary.get(key);
            if (entry != null) {
                return converter.toRest(entry, utils.obtainProjection());
            }
        }

        // Return the first entry if none of the specified keys are found
        if (!formMetadataVocabulary.isEmpty()) {
            DCMetadataVocabulary firstEntry = formMetadataVocabulary.values().iterator().next();
            return converter.toRest(firstEntry, utils.obtainProjection());
        }

        return null;
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<MetadataVocabularyRest> findAll(Context context, Pageable pageable) {
        Locale currentLocale = context.getCurrentLocale();
        DCInputsReader inputReader = inputReaders.getOrDefault(currentLocale, defaultInputReader);
        Map<String, List<DCMetadataVocabulary>> allMetadataVocabularies = inputReader.GetMetadataVocabulary();

        List<DCMetadataVocabulary> aggregatedMetadataVocabularies = new ArrayList<>();
        Set<String> addedMetadataIds = new HashSet<>();

        // Iterate over the keys in order of relevance
        for (String key : METADATA_KEYS) {
            List<DCMetadataVocabulary> vocabularies = allMetadataVocabularies.get(key);
            if (vocabularies != null) {
                for (DCMetadataVocabulary vocabulary : vocabularies) {
                    if (!addedMetadataIds.contains(vocabulary.getMetadataId())) {
                        aggregatedMetadataVocabularies.add(vocabulary);
                        addedMetadataIds.add(vocabulary.getMetadataId());
                    }
                }
            }
        }

        // Process remaining vocabularies that are not covered by the keys
        for (Map.Entry<String, List<DCMetadataVocabulary>> entry : allMetadataVocabularies.entrySet()) {
            if (!Arrays.asList(METADATA_KEYS).contains(entry.getKey())) {
                for (DCMetadataVocabulary vocabulary : entry.getValue()) {
                    if (!addedMetadataIds.contains(vocabulary.getMetadataId())) {
                        aggregatedMetadataVocabularies.add(vocabulary);
                        addedMetadataIds.add(vocabulary.getMetadataId());
                    }
                }
            }
        }

        return converter.toRestPage(aggregatedMetadataVocabularies, pageable, aggregatedMetadataVocabularies.size(), utils.obtainProjection());
    }

    @Override
    public Class<MetadataVocabularyRest> getDomainClass() {
        return MetadataVocabularyRest.class;
    }
}
