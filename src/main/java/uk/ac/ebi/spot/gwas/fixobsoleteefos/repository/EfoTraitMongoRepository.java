package uk.ac.ebi.spot.gwas.fixobsoleteefos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.domain.EfoTrait;


public interface EfoTraitMongoRepository extends MongoRepository<EfoTrait, Long> {
    Page<EfoTrait> findAll(Pageable pageable);
    EfoTrait findByShortForm(String shortForm);
}
