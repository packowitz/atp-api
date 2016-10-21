package nz.pacworx.atp.domain;

public enum SurveyType {
    NUMBER100(100, 2), NUMBER250(250, 2), NUMBER500(500, 3), NUMBER1000(1000, 4), PERMANENT(-1, 5), SECURITY(-1, -1);

    private final int maxAnswers;
    private final int maxAbuse;

    SurveyType(int maxAnswers, int maxAbuse) {
        this.maxAnswers = maxAnswers;
        this.maxAbuse = maxAbuse;
    }

    public int getMaxAnswers() {
        return maxAnswers;
    }

    public int getMaxAbuse() {
        return maxAbuse;
    }
}
