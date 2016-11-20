package io.pacworx.atp.country;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/country")
public class CountryController {

    @Autowired
    private CountryRepository countryRepository;

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Country>> getCountries() {
        List<Country> countries = countryRepository.findByActiveTrueOrderByNameEngAsc();
        return new ResponseEntity<>(countries, HttpStatus.OK);
    }

}
