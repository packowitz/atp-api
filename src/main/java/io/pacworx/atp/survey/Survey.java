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
    private String pic1;
    private String pic2;
    private Integer minAge;
    private Integer maxAge;
    private boolean age_1;
    private boolean age_2;
    private boolean age_3;
    private boolean age_4;
    private boolean age_5;
    private boolean age_6;
    private boolean age_7;
    private boolean age_8;
    private boolean age_9;
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
    @JsonView(Views.WebView.class)
    private Integer daysBetween;

    public boolean hasAgeRange() {
        return age_1 || age_2 || age_3 || age_4 || age_5 || age_6 || age_7 || age_8 || age_9;
    }

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

    public Integer getMinAge() {
        return minAge;
    }

    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isAge_1() {
        return age_1;
    }

    public void setAge_1(boolean age_1) {
        this.age_1 = age_1;
    }

    public boolean isAge_2() {
        return age_2;
    }

    public void setAge_2(boolean age_2) {
        this.age_2 = age_2;
    }

    public boolean isAge_3() {
        return age_3;
    }

    public void setAge_3(boolean age_3) {
        this.age_3 = age_3;
    }

    public boolean isAge_4() {
        return age_4;
    }

    public void setAge_4(boolean age_4) {
        this.age_4 = age_4;
    }

    public boolean isAge_5() {
        return age_5;
    }

    public void setAge_5(boolean age_5) {
        this.age_5 = age_5;
    }

    public boolean isAge_6() {
        return age_6;
    }

    public void setAge_6(boolean age_6) {
        this.age_6 = age_6;
    }

    public boolean isAge_7() {
        return age_7;
    }

    public void setAge_7(boolean age_7) {
        this.age_7 = age_7;
    }

    public boolean isAge_8() {
        return age_8;
    }

    public void setAge_8(boolean age_8) {
        this.age_8 = age_8;
    }

    public boolean isAge_9() {
        return age_9;
    }

    public void setAge_9(boolean age_9) {
        this.age_9 = age_9;
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

    public Integer getDaysBetween() {
        return daysBetween;
    }

    public void setDaysBetween(Integer daysBetween) {
        this.daysBetween = daysBetween;
    }
}
