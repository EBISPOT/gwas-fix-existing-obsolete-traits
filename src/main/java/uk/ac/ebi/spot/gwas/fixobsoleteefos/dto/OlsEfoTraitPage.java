package uk.ac.ebi.spot.gwas.fixobsoleteefos.dto;

import lombok.Data;

import java.util.List;

@Data
public class OlsEfoTraitPage {
    Embedded _embedded;
    Page page;

    @Data
    public static class Embedded {

        private List<OlsEfoTrait> terms;

    }

    @Data
    public static class Page {

        private int size;
        private int totalElements;
        private int totalPages;
        private int number;

    }
}
