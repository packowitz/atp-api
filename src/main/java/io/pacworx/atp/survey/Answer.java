package io.pacworx.atp.survey;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    private long userId;
    private long surveyId;
    private LocalDateTime answerTime = LocalDateTime.now();
    private int answer;
    private int answerId;
    private int age;
    private int ageRange;
    private String country;
    private boolean male;
    private Long surveyGroupId;
    private int pic1_id;
    private int pic2_id;
    private int rating;

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(long surveyId) {
        this.surveyId = surveyId;
    }

    public LocalDateTime getAnswerTime() {
        return answerTime;
    }

    public void setAnswerTime(LocalDateTime answerTime) {
        this.answerTime = answerTime;
    }

    public int getAnswer() {
        return answer;
    }

    public void setAnswer(int answer) {
        this.answer = answer;
    }

    public int getAnswerId() {
        return answerId;
    }

    public void setAnswerId(int answerId) {
        this.answerId = answerId;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(int ageRange) {
        this.ageRange = ageRange;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isMale() {
        return male;
    }

    public void setMale(boolean male) {
        this.male = male;
    }

    public Long getSurveyGroupId() {
        return surveyGroupId;
    }

    public void setSurveyGroupId(Long surveyGroupId) {
        this.surveyGroupId = surveyGroupId;
    }

    public int getPic1_id() {
        return pic1_id;
    }

    public void setPic1_id(int pic1_id) {
        this.pic1_id = pic1_id;
    }

    public int getPic2_id() {
        return pic2_id;
    }

    public void setPic2_id(int pic2_id) {
        this.pic2_id = pic2_id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
