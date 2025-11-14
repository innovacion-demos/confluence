package com.example.demo.domain.model;

import java.util.List;

@Deprecated // Ya no se utiliza; sustituido por McpAnswerResponse
public class AnswerResponse {
    private String question;
    private String answer;
    private List<VectorSearchResult> sources;

    public AnswerResponse(String question, String answer, List<VectorSearchResult> sources) {
        this.question = question;
        this.answer = answer;
        this.sources = sources;
    }

    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public List<VectorSearchResult> getSources() { return sources; }
}
