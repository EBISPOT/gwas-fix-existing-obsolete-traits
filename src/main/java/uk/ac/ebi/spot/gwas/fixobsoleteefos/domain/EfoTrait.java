package uk.ac.ebi.spot.gwas.fixobsoleteefos.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document("efoTraits")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EfoTrait {
    @Id
    private String id;
    @Indexed
    private String trait;
    @Indexed
    private String shortForm;
    @Indexed(unique = true)
    private String uri;
    private Provenance created;
    private Provenance updated;
}
