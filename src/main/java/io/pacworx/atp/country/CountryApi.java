package io.pacworx.atp.country;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Country API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Country", description = "Country APIs")
@RequestMapping("/country")
public interface CountryApi {
    @ApiOperation(value = "List countries",
            notes = "This API returns a list of countries available to ATPs",
            response = Country[].class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<List<Country>> getCountries();
}
