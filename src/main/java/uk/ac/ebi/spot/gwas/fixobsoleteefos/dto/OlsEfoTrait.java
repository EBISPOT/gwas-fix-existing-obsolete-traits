package uk.ac.ebi.spot.gwas.fixobsoleteefos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OlsEfoTrait {
    private String iri;
    private String label;
    @JsonProperty("is_obsolete")
    private boolean isObsolete;
    @JsonProperty("term_replaced_by")
    private String termReplacedBy;
    @JsonProperty("short_form")
    private String shortForm;
}
