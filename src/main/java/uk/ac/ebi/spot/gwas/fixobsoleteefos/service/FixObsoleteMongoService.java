package uk.ac.ebi.spot.gwas.fixobsoleteefos.service;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.domain.EfoTrait;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.dto.OlsEfoTrait;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.dto.OlsEfoTraitPage;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.repository.EfoTraitMongoRepository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FixObsoleteMongoService {
    private final EfoTraitMongoRepository efoTraitMongoRepository;
    private final MongoTemplate mongoTemplate;

    public FixObsoleteMongoService(EfoTraitMongoRepository efoTraitMongoRepository, MongoTemplate mongoTemplate) {
        this.efoTraitMongoRepository = efoTraitMongoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void fixObsoleteTraits() throws UnsupportedEncodingException {
        RestTemplate restTemplate = new RestTemplate();
        OlsEfoTraitPage olsEfoTraitPage = restTemplate.getForObject("https://www.ebi.ac.uk/ols4/api/ontologies/efo/terms?obsoletes=true&size=100", OlsEfoTraitPage.class);
        int totalPages = olsEfoTraitPage.getPage().getTotalPages();
        for (int i = 0; i < totalPages; i++) {
            List<OlsEfoTrait> olsEfoTraits = restTemplate.getForObject("https://www.ebi.ac.uk/ols4/api/ontologies/efo/terms?obsoletes=true&size=100&page=" + i,
                    OlsEfoTraitPage.class).get_embedded().getTerms();
            for(OlsEfoTrait olsEfoTrait: olsEfoTraits) {
                if (olsEfoTrait.getShortForm() == null || olsEfoTrait.getTermReplacedBy() == null) continue;
                EfoTrait obsoleteTerm = efoTraitMongoRepository.findByShortForm(olsEfoTrait.getShortForm());
                if (obsoleteTerm == null) continue;
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
                EfoTrait replacementTerm = efoTraitMongoRepository.findByShortForm(replacedBy);
                if (replacementTerm == null) continue;
                System.out.printf("Found %s to be replaced by %s\n",obsoleteTerm.getUri(), replacementTerm.getUri());
                triggerReplace("studies", "efoTraits", obsoleteTerm.getId(), replacementTerm.getId());
                triggerReplace("studies", "backgroundEfoTraits", obsoleteTerm.getId(), replacementTerm.getId());
                efoTraitMongoRepository.delete(obsoleteTerm);
            }
        }
    }

    private void triggerReplace(String collectionName, String fieldName, String obsoleteId, String replacementId) {
        System.out.println("replacing " + obsoleteId + " by " + replacementId + " in " + collectionName + "." + fieldName);
        Query query = new Query(Criteria.where(fieldName).is(obsoleteId));
        Update update = new Update()
                .set(fieldName + ".$[element]", replacementId)
                .filterArray(Criteria.where("element").is(obsoleteId));
        UpdateResult result = mongoTemplate.updateMulti(query, update, collectionName);
        System.out.println("Number of documents matched: " + result.getMatchedCount());
        System.out.println("Number of documents updated: " + result.getModifiedCount());
    }
}
