package io.pacworx.atp.country;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CountryController implements CountryApi {

    private final CountryRepository countryRepository;

    @Autowired
    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public ResponseEntity<List<Country>> getCountries() {
        List<Country> countries = countryRepository.findByActiveTrueOrderByNameEngAsc();
        return new ResponseEntity<>(countries, HttpStatus.OK);
    }
}
