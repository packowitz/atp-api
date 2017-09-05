package io.pacworx.atp.user;

import java.time.LocalDate;

public enum AgeRange {
    KIDS(1, "Kids 9 and under"),
    PRETEENS(2, "Kids from 10 to 12"),
    YOUNGTEENS(3, "Teens from 13 to 15"),
    TEENS(4, "Teens from 16 to 17"),
    SENIORTEENS(5, "Teens from 18 to 21"),
    YOUNGADULTS(6, "Adults from 22 to 29"),
    ADULTS(7, "Adults from 30 to 39"),
    SENIOSADULTS(8, "Adults from 40 to 55"),
    ELDERLY(9, "Adults older than 55");

    private int id;
    private String description;

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    AgeRange(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public static AgeRange byId(int id) {
        for(AgeRange ageRange: values()) {
            if(ageRange.id == id) {
                return ageRange;
            }
        }
        return null;
    }

    public static AgeRange byYearOfBirth(int yearOfBirth) {
        int age = LocalDate.now().getYear() - yearOfBirth;
        if(age <= 9) return KIDS;
        if(age <= 12) return PRETEENS;
        if(age <= 15) return YOUNGTEENS;
        if(age <= 17) return TEENS;
        if(age <= 21) return SENIORTEENS;
        if(age <= 29) return YOUNGADULTS;
        if(age <= 39) return ADULTS;
        if(age <= 55) return SENIOSADULTS;
        return ELDERLY;
    }
}
