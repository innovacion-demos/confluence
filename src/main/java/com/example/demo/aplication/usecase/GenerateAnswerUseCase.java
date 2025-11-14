package com.example.demo.aplication.usecase;

import com.example.demo.domain.model.McpAnswerResponse;
import com.example.demo.domain.model.VectorSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GenerateAnswerUseCase {

    private final SearchSimilarPagesUseCase searchSimilarPagesUseCase;

    public GenerateAnswerUseCase(SearchSimilarPagesUseCase searchSimilarPagesUseCase) {
        this.searchSimilarPagesUseCase = searchSimilarPagesUseCase;
    }

    public McpAnswerResponse execute(String question) {
        List<VectorSearchResult> results = searchSimilarPagesUseCase.execute(question);
        if (results.isEmpty()) {
            return new McpAnswerResponse(question, "No se encontraron fuentes relevantes.", results);
        }
        // Construir respuesta sencilla tomando fragmentos iniciales de content
        String answer = results.stream()
                .map(r -> extraerFragmento(r.getContent()))
                .filter(f -> !f.isBlank())
                .distinct()
                .collect(Collectors.joining(" \n\n"));
        if (answer.isBlank()) {
            answer = "Se encontraron páginas pero sin contenido utilizable.";
        }
        return new McpAnswerResponse(question, answer, results);
    }

    private String extraerFragmento(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        int max = 400;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }
}
