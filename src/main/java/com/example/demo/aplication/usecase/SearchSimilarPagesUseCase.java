package com.example.demo.aplication.usecase;

import com.example.demo.aplication.port.EmbeddingRepository;
import com.example.demo.aplication.port.VectorSearchRepository;
import com.example.demo.domain.model.VectorSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchSimilarPagesUseCase {

    private final EmbeddingRepository embeddingRepository;
    private final VectorSearchRepository vectorSearchRepository;

    public SearchSimilarPagesUseCase(EmbeddingRepository embeddingRepository, VectorSearchRepository vectorSearchRepository) {
        this.embeddingRepository = embeddingRepository;
        this.vectorSearchRepository = vectorSearchRepository;
    }

    public List<VectorSearchResult> execute(String question) {
        List<Double> queryVector = embeddingRepository.vectorize(question);
        return vectorSearchRepository.searchSimilar(queryVector);
    }
}
