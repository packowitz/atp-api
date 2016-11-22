package io.pacworx.atp.survey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Entity
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long userId;
    @Enumerated(EnumType.STRING)
    private SurveyType type;
    @Enumerated(EnumType.STRING)
    private SurveyStatus status;
    private String title;
    @NotNull
    private String pic1;
    @NotNull
    private String pic2;
    @NotNull
    private int minAge;
    @NotNull
    private int maxAge;
    @NotNull
    private String countries;
    @NotNull
    private boolean male;
    @NotNull
    private boolean female;
    private int answered;
    private int noOpinionCount;
    private int pic1Count;
    private int pic2Count;
    private int abuseCount;
    private ZonedDateTime startedDate;
    @JsonIgnore
    private ZonedDateTime updatedDate;
    @JsonIgnore
    private int maxAnswers;
    @JsonIgnore
    private int maxAbuse;
    @JsonView(Views.WebView.class)
    private Integer expectedAnswer;
    private Long groupId;
    private int pic1_id = 1;
    private int pic2_id = 2;
    private boolean multiPicture = false;

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public SurveyType getType() {
        return type;
    }

    public void setType(SurveyType type) {
        this.type = type;
        this.maxAnswers = type.getMaxAnswers();
        this.maxAbuse = type.getMaxAbuse();
    }

    public SurveyStatus getStatus() {
        return status;
    }

    public void setStatus(SurveyStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPic1() {
        return pic1;
    }

    public void setPic1(String pic1) {
        this.pic1 = pic1;
    }

    public String getPic2() {
        return pic2;
    }

    public void setPic2(String pic2) {
        this.pic2 = pic2;
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public String getCountries() {
        return countries;
    }

    public void setCountries(String countries) {
        this.countries = countries;
    }

    public boolean isMale() {
        return male;
    }

    public void setMale(boolean male) {
        this.male = male;
    }

    public boolean isFemale() {
        return female;
    }

    public void setFemale(boolean female) {
        this.female = female;
    }

    public int getAnswered() {
        return answered;
    }

    public int getNoOpinionCount() {
        return noOpinionCount;
    }

    public int getPic1Count() {
        return pic1Count;
    }

    public int getPic2Count() {
        return pic2Count;
    }

    public int getAbuseCount() {
        return abuseCount;
    }

    public ZonedDateTime getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(ZonedDateTime startedDate) {
        this.startedDate = startedDate;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    public int getMaxAnswers() {
        return maxAnswers;
    }

    public int getMaxAbuse() {
        return maxAbuse;
    }

    public Integer getExpectedAnswer() {
        return expectedAnswer;
    }

    public void setExpectedAnswer(Integer expectedAnswer) {
        this.expectedAnswer = expectedAnswer;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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

    public boolean isMultiPicture() {
        return multiPicture;
    }

    public void setMultiPicture(boolean multiPicture) {
        this.multiPicture = multiPicture;
    }
}
