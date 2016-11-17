package io.pacworx.atp.domain;

public enum SurveyType {
    NUMBER100(100, 1000, 5, 2), NUMBER300(300, 2900, 5, 2), NUMBER1000(1000, 9000, 5, 4), PERMANENT(-1, 0, 2, 10), SECURITY(-1, 0, 2, -1);

    private final int maxAnswers;
    private final int creationCosts;
    private final int answerReward;
    private final int maxAbuse;

    SurveyType(int maxAnswers, int creationCosts, int answerReward, int maxAbuse) {
        this.maxAnswers = maxAnswers;
        this.creationCosts = creationCosts;
        this.answerReward = answerReward;
        this.maxAbuse = maxAbuse;
    }

    public int getMaxAnswers() {
        return maxAnswers;
    }

    public int getCreationCosts() {
        return creationCosts;
    }

    public int getAnswerReward() {
        return answerReward;
    }

    public int getMaxAbuse() {
        return maxAbuse;
    }
}
