package com.example.slagalica.domain.models;

import java.util.List;

public class MatchingData {
    private String title;
    private List<String> left_items;
    private List<String> right_items;
    private List<Long> correct_pairs;

    public MatchingData() {}

    public String getTitle() { return title; }
    public List<String> getLeft_items() { return left_items; }
    public List<String> getRight_items() { return right_items; }
    public List<Long> getCorrect_pairs() { return correct_pairs; }

    public void setTitle(String title) { this.title = title; }
    public void setLeft_items(List<String> left_items) { this.left_items = left_items; }
    public void setRight_items(List<String> right_items) { this.right_items = right_items; }
    public void setCorrect_pairs(List<Long> correct_pairs) { this.correct_pairs = correct_pairs; }
    public List<String> getLeft() { return left_items; }
    public List<String> getRight() { return right_items; }
    public List<Long> getCorrectMap() { return correct_pairs; }
}