package com.example.demo.infrastructure.mcp;

public class PageInfo {
    private String id;
    private String title;
    private String url;
    private String content;
    private Double score;

    public PageInfo(String id, String title, String url, String content, Double score) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.content = content;
        this.score = score;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getContent() { return content; }
    public Double getScore() { return score; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setContent(String content) { this.content = content; }
    public void setScore(Double score) { this.score = score; }
}

