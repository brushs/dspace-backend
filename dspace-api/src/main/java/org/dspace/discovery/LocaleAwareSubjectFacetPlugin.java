package org.dspace.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.FacetParams;
import org.dspace.core.Context;

/**
 * DSpace 7.3: Locale-aware toggle between subject_broad_en and subject_broad_fr facets.
 * Works entirely server-side (no Angular changes required).
 *
 * Wire as a Spring bean in the API context. See XML below.
 */
public class LocaleAwareSubjectFacetPlugin implements SolrServiceSearchPlugin {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SolrServiceImpl.class);

    private static final String FACET_TO_REPLACE = "bi_4_dis";

    private static final String FACET_EN = "subject_en";
    private static final String FACET_FR = "subject_fr";

    /** Primary language codes that should select the FR facet. */
    private List<String> frPrimaries = Arrays.asList("fr");

    /** Optional setter from Spring if you want frPrimaries=["fr","fr_CA"] etc. */
    public void setFrPrimaries(List<String> frPrimaries) {
        if (frPrimaries != null && !frPrimaries.isEmpty()) {
            this.frPrimaries = frPrimaries;
        }
    }

    public LocaleAwareSubjectFacetPlugin() {
        log.warn(">>> Loaded LocaleAwareSubjectFacetPlugin");
    }

    @Override
    public void additionalSearchParameters(Context context,
                                           DiscoverQuery discoverQuery,
                                           SolrQuery solrQuery) {

        String inactive = FACET_TO_REPLACE;
        String inactiveFilter = inactive + "_filter";

        // 1) Rewrite facet.field list: drop inactive, ensure active is present
        String[] existing = solrQuery.getFacetFields();

        if (existing == null || existing.length != 1) {
            return;
        }

        if (!existing[0].equals(inactiveFilter)) {
            return;
        }

        String primary = getPrimaryLanguage(context);
        boolean useFr = frPrimaries.contains(primary);
        String active   = useFr ? FACET_FR : FACET_EN;
        String activeFilter = active + "_filter";

        // reset facet.field params
        solrQuery.remove(FacetParams.FACET_FIELD);
        solrQuery.addFacetField(activeFilter);

        // 2) Move per-facet params (limit/sort/offset/prefix) from inactive -> active (if set)
        copyPerFacetParam(solrQuery, inactiveFilter, activeFilter, FacetParams.FACET_LIMIT);
        copyPerFacetParam(solrQuery, inactiveFilter, activeFilter, FacetParams.FACET_SORT);
        copyPerFacetParam(solrQuery, inactiveFilter, activeFilter, FacetParams.FACET_OFFSET);
        copyPerFacetParam(solrQuery, inactiveFilter, activeFilter, FacetParams.FACET_PREFIX);

        // Remove any stray per-facet params for the inactive facet
        removePerFacetParams(solrQuery, inactiveFilter,
                FacetParams.FACET_LIMIT,
                FacetParams.FACET_SORT,
                FacetParams.FACET_OFFSET,
                FacetParams.FACET_PREFIX);

        // 3) Rewrite incoming facet filter queries to the active facet
        String[] fqs = solrQuery.getFilterQueries();
        if (fqs != null && fqs.length > 0) {
            for (int i = 0; i < fqs.length; i++) {
                // Replace both "f.<field>=..." and "<field>:"
                fqs[i] = fqs[i]
                        .replace("f." + inactiveFilter + "=", "f." + activeFilter + "=")
                        .replace(inactiveFilter + ":", activeFilter + ":");
            }
            solrQuery.setFilterQueries(fqs);
        }
    }

    private String getPrimaryLanguage(Context context) {
        if (context != null && context.getCurrentLocale() != null) {
            String code = context.getCurrentLocale().getLanguage();
            if (StringUtils.isNotBlank(code)) {
                return code.toLowerCase(Locale.ROOT);
            }
        }
        return "en";
    }

    private void copyPerFacetParam(SolrQuery q, String fromField, String toField, String param) {
        String fromKey = "f." + fromField + "." + param;
        String toKey   = "f." + toField   + "." + param;
        String[] vals = q.getParams(fromKey);
        if (vals != null && vals.length > 0 && q.get(toKey) == null) {
            q.set(toKey, vals);
        }
    }

    private void removePerFacetParams(SolrQuery q, String field, String... params) {
        for (String p : params) {
            q.remove("f." + field + "." + p);
        }
    }
}
