package com.example.demo.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class McpQueryResponse {
    private String type = "query_results";
    private String query;
    private List<VectorSearchResult> results;
    private Map<String,Object> meta;

    public McpQueryResponse(String query, List<VectorSearchResult> results) {
        this.query = query;
        this.results = results;
        this.meta = Map.of(
                "generated_at", Instant.now().toString(),
                "engine", "qdrant",
                "count", results.size()
        );
    }

    public String getType() { return type; }
    public String getQuery() { return query; }
    public List<VectorSearchResult> getResults() { return results; }
    public Map<String,Object> getMeta() { return meta; }
}

