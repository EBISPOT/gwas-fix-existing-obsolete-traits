package uk.ac.ebi.spot.gwas.fixobsoleteefos;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.service.FixObsoleteMongoService;
import uk.ac.ebi.spot.gwas.fixobsoleteefos.service.FixObsoleteOracleService;

import java.io.UnsupportedEncodingException;

@Component
public class ApplicationRunner implements CommandLineRunner {
    private final FixObsoleteMongoService fixObsoleteMongoService;
    private final FixObsoleteOracleService fixObsoleteOracleService;

    public ApplicationRunner(FixObsoleteMongoService fixObsoleteMongoService, FixObsoleteOracleService fixObsoleteOracleService) {
        this.fixObsoleteMongoService = fixObsoleteMongoService;
        this.fixObsoleteOracleService = fixObsoleteOracleService;
    }

    @Override
    public void run(String... args) throws UnsupportedEncodingException {
        if (args.length > 0) {
            String scenario = args[0].toLowerCase();
            switch (scenario) {
                case "mongo":
                    fixObsoleteMongoService.fixObsoleteTraits();
                    break;
                case "oracle":
					fixObsoleteOracleService.fixObsoleteTraits();
                    break;
                default:
                    System.out.println("Invalid args. Please use 'mongo' or 'oracle'.");
            }
        } else {
            System.out.println("No scenario provided. Please specify 'mongo' or 'oracle'.");
        }
    }
}
