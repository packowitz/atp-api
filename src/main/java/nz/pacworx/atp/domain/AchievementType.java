package nz.pacworx.atp.domain;

public enum AchievementType {
    ACTIVE_USER(50), CHOOSE_USERNAME(500), ATP_CREATOR(500, 1000, 2000), ATP_ANSWERER(250, 500, 1000), RELIABLE_USER(250, 500, 1000);

    private int step;
    private int step1reward;
    private int step2reward;
    private int step3reward;

    AchievementType(int reward) {
        this.step = 3;
        this.step3reward = reward;
    }

    AchievementType(int step1reward, int step2reward, int step3reward) {
        this.step1reward = step1reward;
        this.step2reward = step2reward;
        this.step3reward = step3reward;
    }

    public int getStep() {
        return step;
    }

    public int getReward(int step) {
        if(step == 1) {
            return step1reward;
        }
        if(step == 2) {
            return  step2reward;
        }
        if(step == 3) {
            return step3reward;
        }
        return 0;
    }
}
