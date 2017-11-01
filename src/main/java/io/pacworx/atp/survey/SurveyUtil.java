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

    private final CountryRepository countryRepository;

    @Autowired
    public SurveyUtil(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public List<Survey> generateMultiPictureSurveys(Survey blueprint, List<String> pictures, boolean eachCountrySeparat) {
        boolean multipicture = pictures.size() > 2;
        List<Survey> surveys = new ArrayList<>();

        for(int pic1_id = 0; (pic1_id + 1) < pictures.size(); pic1_id++) {
            for(int pic2_id = pic1_id + 1; pic2_id < pictures.size(); pic2_id++) {
                List<String> countries = resolveCountries(blueprint.getCountries(), eachCountrySeparat);
                for(String country: countries) {
                    Survey survey = new Survey();
                    survey.setUserId(blueprint.getUserId());
                    survey.setType(blueprint.getType());
                    if(survey.getType() == SurveyType.SECURITY) {
                        survey.setExpectedAnswer(blueprint.getExpectedAnswer());
                    }
                    survey.setStatus(SurveyStatus.ACTIVE);
                    survey.setStartedDate(blueprint.getStartedDate());

                    survey.setTitle(blueprint.getTitle());
                    survey.setMale(blueprint.isMale());
                    survey.setFemale(blueprint.isFemale());
                    if(blueprint.getMinAge() != null && blueprint.getMaxAge() != null) {
                        survey.setMinAge(blueprint.getMinAge());
                        survey.setMaxAge(blueprint.getMaxAge());
                        survey.setAge_1(blueprint.getMinAge() <= 9);
                        survey.setAge_2(blueprint.getMaxAge() >= 10 && blueprint.getMinAge() <= 12);
                        survey.setAge_3(blueprint.getMaxAge() >= 13 && blueprint.getMinAge() <= 15);
                        survey.setAge_4(blueprint.getMaxAge() >= 16 && blueprint.getMinAge() <= 17);
                        survey.setAge_5(blueprint.getMaxAge() >= 18 && blueprint.getMinAge() <= 21);
                        survey.setAge_6(blueprint.getMaxAge() >= 22 && blueprint.getMinAge() <= 29);
                        survey.setAge_7(blueprint.getMaxAge() >= 30 && blueprint.getMinAge() <= 39);
                        survey.setAge_8(blueprint.getMaxAge() >= 40 && blueprint.getMinAge() <= 55);
                        survey.setAge_9(blueprint.getMaxAge() >= 56);
                    } else {
                        survey.setAge_1(blueprint.isAge_1());
                        survey.setAge_2(blueprint.isAge_2());
                        survey.setAge_3(blueprint.isAge_3());
                        survey.setAge_4(blueprint.isAge_4());
                        survey.setAge_5(blueprint.isAge_5());
                        survey.setAge_6(blueprint.isAge_6());
                        survey.setAge_7(blueprint.isAge_7());
                        survey.setAge_8(blueprint.isAge_8());
                        survey.setAge_9(blueprint.isAge_9());
                        survey.setMinAge(5);
                        survey.setMaxAge(99);
                    }
                    survey.setCountries(country);
                    survey.setDaysBetween(blueprint.getDaysBetween());

                    survey.setMultiPicture(multipicture);

                    //randomize which picture is left and right
                    if(!multipicture || random.nextDouble() < 0.5) {
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
