package com.example.demo.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class McpAnswerResponse {
    private String type = "answer";
    private String question;
    private String answer;
    private List<VectorSearchResult> sources;
    private Map<String,Object> meta;

    public McpAnswerResponse(String question, String answer, List<VectorSearchResult> sources) {
        this.question = question;
        this.answer = answer;
        this.sources = sources;
        this.meta = Map.of(
                "generated_at", Instant.now().toString(),
                "engine", "qdrant",
                "source_count", sources.size()
        );
    }

    public String getType() { return type; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public List<VectorSearchResult> getSources() { return sources; }
    public Map<String,Object> getMeta() { return meta; }
}

