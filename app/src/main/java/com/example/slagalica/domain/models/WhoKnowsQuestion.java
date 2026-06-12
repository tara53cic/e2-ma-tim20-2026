package com.example.slagalica.domain.models;

import java.util.List;

public class WhoKnowsQuestion {
    private String question;
    private List<String> answers;
    private int correct_index;
    private String category;

    public WhoKnowsQuestion() {}

    public String getQuestion() { return question; }
    public List<String> getAnswers() { return answers; }
    public int getCorrect_index() { return correct_index; }
    public String getCategory() { return category; }

    public void setQuestion(String question) { this.question = question; }
    public void setAnswers(List<String> answers) { this.answers = answers; }
    public void setCorrect_index(int correct_index) { this.correct_index = correct_index; }
    public void setCategory(String category) { this.category = category; }
    public int getCorrectIndex() { return correct_index; }
}