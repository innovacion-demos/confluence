package com.example.demo.domain.model;


public class VectorSearchResult {
    private String id;
    private String title;
    private String url;
    private String content;
    private double score;


    public VectorSearchResult(String id, String title, String url, String content, double score) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.content = content;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
