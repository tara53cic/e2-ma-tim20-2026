package com.example.slagalica.domain.models;

public class StepByStep {
    private String step_1;
    private String step_2;
    private String step_3;
    private String step_4;
    private String step_5;
    private String step_6;
    private String step_7;
    private String final_answer;

    public StepByStep() {
    }

    public StepByStep(String step_1, String step_2, String step_3, String step_4, String step_5, String step_6, String step_7, String final_answer) {
        this.step_1 = step_1;
        this.step_2 = step_2;
        this.step_3 = step_3;
        this.step_4 = step_4;
        this.step_5 = step_5;
        this.step_6 = step_6;
        this.step_7 = step_7;
        this.final_answer = final_answer;
    }

    public String getStep(int i) {
        switch (i) {
            case 1: return step_1;
            case 2: return step_2;
            case 3: return step_3;
            case 4: return step_4;
            case 5: return step_5;
            case 6: return step_6;
            case 7: return step_7;
            default: return "";
        }
    }

    public String getStep_1() { return step_1; }
    public String getStep_2() { return step_2; }
    public String getStep_3() { return step_3; }
    public String getStep_4() { return step_4; }
    public String getStep_5() { return step_5; }
    public String getStep_6() { return step_6; }
    public String getStep_7() { return step_7; }
    public String getFinal_answer() { return final_answer; }
}
