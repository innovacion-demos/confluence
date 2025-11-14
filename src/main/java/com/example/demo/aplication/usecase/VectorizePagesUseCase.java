package com.example.demo.aplication.usecase;

import com.example.demo.aplication.port.ContentFetcherPort;
import com.example.demo.aplication.port.EmbeddingRepository;
import com.example.demo.aplication.port.VectorSearchRepository;
import com.example.demo.domain.model.ConfluencePage;
import com.example.demo.domain.model.VectorizedPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class VectorizePagesUseCase {

    private final ContentFetcherPort contentFetcherPort;
    private final EmbeddingRepository embeddingRepository;
    private final VectorSearchRepository vectorSearchRepository;

    public VectorizePagesUseCase(
        ContentFetcherPort contentFetcherPort,
        EmbeddingRepository embeddingRepository,
        VectorSearchRepository vectorSearchRepository
    ) {
        this.contentFetcherPort = contentFetcherPort;
        this.embeddingRepository = embeddingRepository;
        this.vectorSearchRepository = vectorSearchRepository;
    }

    public List<VectorizedPage> execute() {
        List<ConfluencePage> pages = contentFetcherPort.fetchAllPages();
        List<VectorizedPage> vectorizedPages = new ArrayList<>();

        for (ConfluencePage page : pages) {
            String fullText = page.getTitle() + " " + page.getContent();
            List<Double> vector = embeddingRepository.vectorize(fullText);

            VectorizedPage vp = new VectorizedPage();
            vp.setId(page.getId());
            vp.setTitle(page.getTitle());
            vp.setContent(page.getContent());
            vp.setUrl(page.getUrl());
            vp.setVector(vector);
            vectorizedPages.add(vp);
            vectorSearchRepository.save(vp);
        }

        return vectorizedPages;
    }
}