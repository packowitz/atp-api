package io.pacworx.atp.survey;

import io.pacworx.atp.country.Country;
import io.pacworx.atp.country.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SurveyUtil {

    private Random random = new Random();

    @Autowired
    private CountryRepository countryRepository;

    public List<Survey> generateMultiPictureSurveys(Survey blueprint, List<String> pictures, boolean eachCountrySeparat) {
        List<Survey> surveys = new ArrayList<>();

        for(int pic1_id = 0; (pic1_id + 1) < pictures.size(); pic1_id++) {
            for(int pic2_id = pic1_id + 1; pic2_id < pictures.size(); pic2_id++) {
                List<String> countries = resolveCountries(blueprint.getCountries(), eachCountrySeparat);
                for(String country: countries) {
                    Survey survey = new Survey();
                    survey.setUserId(blueprint.getUserId());
                    survey.setType(blueprint.getType());
                    survey.setStatus(SurveyStatus.ACTIVE);
                    survey.setStartedDate(blueprint.getStartedDate());

                    survey.setTitle(blueprint.getTitle());
                    survey.setMale(blueprint.isMale());
                    survey.setFemale(blueprint.isFemale());
                    survey.setMinAge(blueprint.getMinAge());
                    survey.setMaxAge(blueprint.getMaxAge());
                    survey.setCountries(country);

                    survey.setMultiPicture(true);

                    //randomize which picture is left and right
                    if(random.nextDouble() < 0.5) {
                        survey.setPic1_id(pic1_id + 1);
                        survey.setPic1(pictures.get(pic1_id));
                        survey.setPic2_id(pic2_id + 1);
                        survey.setPic2(pictures.get(pic2_id));
                    } else {
                        survey.setPic1_id(pic2_id + 1);
                        survey.setPic1(pictures.get(pic2_id));
                        survey.setPic2_id(pic1_id + 1);
                        survey.setPic2(pictures.get(pic1_id));
                    }
                    surveys.add(survey);
                }
            }
        }

        return surveys;
    }

    private List<String> resolveCountries(String inputString, boolean eachCountrySeparat) {
        List<String> countries = new ArrayList<>();
        if(!eachCountrySeparat) {
            countries.add(inputString);
        } else {
            if(inputString.equals("ALL")) {
                for(Country country: countryRepository.findByActiveTrueOrderByNameEngAsc()) {
                    countries.add(country.getAlpha3());
                }
            } else {
                for(String country: inputString.split(",")) {
                    countries.add(country);
                }
            }
        }
        return countries;
    }
}
