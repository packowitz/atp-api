package io.pacworx.atp.country;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CountryController implements CountryApi {
    private static Logger log = LogManager.getLogger();

    private final CountryRepository countryRepository;

    @Autowired
    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public ResponseEntity<List<Country>> getCountries() {
        List<Country> countries = countryRepository.findByActiveTrueOrderByNameEngAsc();
        log.info("get countries was called");
        return new ResponseEntity<>(countries, HttpStatus.OK);
    }
}
