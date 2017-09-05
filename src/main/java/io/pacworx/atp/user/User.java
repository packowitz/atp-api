package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.survey.Survey;
import io.pacworx.atp.survey.SurveyType;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Entity
@Table(name="`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.WebView.class)
    private long id;
    private String username;
    @JsonIgnore
    private String password;
    private String email;
    private boolean emailConfirmed;
    @Min(1900)
    @Max(2050)
    @Column(name = "yearofbirth")
    private Integer yearOfBirth;
    private Integer ageRange;
    private String country;
    private Boolean male;
    private int credits = 2000;
    @JsonView(Views.WebView.class)
    private int reliableScore = 100;
    private long surveysAnswered = 0;
    private long surveysStarted = 0;
    private long surveysAnsweredWeek = 0;
    private long surveysStartedWeek = 0;
    @JsonView(Views.WebView.class)
    private LocalDateTime lastLoginTime = LocalDateTime.now();
    @JsonIgnore
    private Long surveyIdToAnswer;
    @JsonIgnore
    private Integer surveyExpectedAnswer;
    @JsonIgnore
    private LocalDateTime surveyAskTime;
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private SurveyType surveyType;
    @JsonIgnore
    private Long surveyGroupId;
    @JsonIgnore
    private Integer surveyPic1_id;
    @JsonIgnore
    private Integer surveyPic2_id;

    public String toString() {
        return (username != null ? username : "anonymous") + " #" + id;
    }

    public void setPassword(String password) throws Exception {
        this.password = getHash(password);
    }

    public boolean passwordMatches(String password) throws Exception {
        return this.password != null && this.password.equals(getHash(password));
    }

    private String getHash(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] array = md.digest((username + "546587toijomklvjfdhuyh" + password + "cnsjdcksnjkr4").getBytes());
        String string = "";
        for(byte aByte : array) {
            string += Integer.toHexString((aByte & 0xFF) | 0x100).substring(1,3);
        }
        return string;
    }

    public void setSurveyToAnswer(Survey survey) {
        if(survey != null) {
            this.surveyIdToAnswer = survey.getId();
            this.surveyAskTime = LocalDateTime.now();
            this.surveyType = survey.getType();
            this.surveyExpectedAnswer = survey.getExpectedAnswer();
            this.surveyGroupId = survey.getGroupId();
            this.surveyPic1_id = survey.getPic1_id();
            this.surveyPic2_id = survey.getPic2_id();
        } else {
            this.surveyIdToAnswer = null;
            this.surveyAskTime = null;
            this.surveyType = null;
            this.surveyExpectedAnswer = null;
            this.surveyGroupId = null;
            this.surveyPic1_id = null;
            this.surveyPic2_id = null;
        }
    }

    public void addCredits(int add) {
        this.credits += add;
    }

    public void incSurveysAnswered() {
        this.surveysAnswered ++;
        this.surveysAnsweredWeek ++;
    }

    public void incSurveysStarted() {
        this.surveysStarted ++;
        this.surveysStartedWeek ++;
    }

    public void incReliableScore(int score) {
        this.reliableScore += score;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public void setEmailConfirmed(boolean emailConfirmed) {
        this.emailConfirmed = emailConfirmed;
    }

    public Integer getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(Integer yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public Integer getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(Integer ageRange) {
        this.ageRange = ageRange;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Boolean isMale() {
        return male;
    }

    public void setMale(Boolean male) {
        this.male = male;
    }

    public int getCredits() {
        return credits;
    }

    public int getReliableScore() {
        return reliableScore;
    }

    public long getSurveysAnswered() {
        return surveysAnswered;
    }

    public long getSurveysStarted() {
        return surveysStarted;
    }

    public long getSurveysAnsweredWeek() {
        return surveysAnsweredWeek;
    }

    public long getSurveysStartedWeek() {
        return surveysStartedWeek;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Long getSurveyIdToAnswer() {
        return surveyIdToAnswer;
    }

    public Integer getSurveyExpectedAnswer() {
        return surveyExpectedAnswer;
    }

    public void setSurveyExpectedAnswer(Integer surveyExpectedAnswer) {
        this.surveyExpectedAnswer = surveyExpectedAnswer;
    }

    public LocalDateTime getSurveyAskTime() {
        return surveyAskTime;
    }

    public SurveyType getSurveyType() {
        return surveyType;
    }

    public Long getSurveyGroupId() {
        return surveyGroupId;
    }

    public void setSurveyGroupId(Long surveyGroupId) {
        this.surveyGroupId = surveyGroupId;
    }

    public Integer getSurveyPic1_id() {
        return surveyPic1_id;
    }

    public void setSurveyPic1_id(Integer surveyPic1_id) {
        this.surveyPic1_id = surveyPic1_id;
    }

    public Integer getSurveyPic2_id() {
        return surveyPic2_id;
    }

    public void setSurveyPic2_id(Integer surveyPic2_id) {
        this.surveyPic2_id = surveyPic2_id;
    }
}
