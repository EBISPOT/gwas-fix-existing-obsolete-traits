package uk.ac.ebi.spot.gwas.fixobsoleteefos.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.dto.OlsEfoTrait;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.dto.OlsEfoTraitPage;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FixObsoleteOracleService {
    private final JdbcTemplate jdbcTemplate;

    public FixObsoleteOracleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void fixObsoleteTraits() throws UnsupportedEncodingException {
        RestTemplate restTemplate = new RestTemplate();
        OlsEfoTraitPage olsEfoTraitPage = restTemplate.getForObject("https://www.ebi.ac.uk/ols4/api/ontologies/efo/terms?obsoletes=true&size=100", OlsEfoTraitPage.class);
        int totalPages = olsEfoTraitPage.getPage().getTotalPages();
        for (int i = 0; i < totalPages; i++) {
            List<OlsEfoTrait> olsEfoTraits = restTemplate.getForObject("https://www.ebi.ac.uk/ols4/api/ontologies/efo/terms?obsoletes=true&size=100&page=" + i,
                    OlsEfoTraitPage.class).get_embedded().getTerms();
            for (OlsEfoTrait olsEfoTrait : olsEfoTraits) {
                if (olsEfoTrait.getShortForm() == null || olsEfoTrait.getTermReplacedBy() == null) continue;
                Long obsoleteId = getEfoIdByShortForm(olsEfoTrait.getShortForm());
                if (obsoleteId == null) continue;
                String replacedBy = olsEfoTrait.getTermReplacedBy();
                if (replacedBy.startsWith("http")) {
                    String efoEncodedUri = URLEncoder.encode(olsEfoTrait.getTermReplacedBy(), StandardCharsets.UTF_8.toString());
                    try {
                        replacedBy = restTemplate.getForObject("https://www.ebi.ac.uk/ols4/api/ontologies/efo/terms/" + efoEncodedUri, OlsEfoTrait.class).getShortForm();
                    }
                    catch (Exception e) {
                        System.out.println(olsEfoTrait.getShortForm() + " " + e.getMessage());
                        continue;
                    }
                } else if (replacedBy.contains(":")) {
                    replacedBy = replacedBy.replace(":", "_");
                }
                Long replacementId = getEfoIdByShortForm(replacedBy);
                if (replacementId == null) continue;
                System.out.printf("Found %s to be replaced by %s\n", olsEfoTrait.getShortForm(), replacedBy);
                System.out.println(obsoleteId + " -> " + replacementId);
                updateJoinTable("STUDY_EFO_TRAIT", obsoleteId, replacementId);
                updateJoinTable("STUDY_BACKGROUND_EFO_TRAIT", obsoleteId, replacementId);
                updateJoinTable("ASSOCIATION_EFO_TRAIT", obsoleteId, replacementId);
                updateJoinTable("ASSOCIATION_BKG_EFO_TRAIT", obsoleteId, replacementId);
            }
        }
    }

    private Long getEfoIdByShortForm(String shortForm) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM EFO_TRAIT WHERE SHORT_FORM = ?",
                    Long.class,
                    shortForm
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void updateJoinTable(String tableName, Long obsoleteId, Long replacementId) {
        jdbcTemplate.update(
                String.format("UPDATE %s SET efo_trait_id = ? WHERE efo_trait_id = ?", tableName),
                replacementId,
                obsoleteId
        );
    }
}
