package com.example.demo.infrastructure.controller;

import com.example.demo.aplication.usecase.FindAllPagesUseCase;
import com.example.demo.aplication.usecase.SearchSimilarPagesUseCase;
import com.example.demo.aplication.usecase.VectorizePagesUseCase;
import com.example.demo.domain.model.ConfluencePage;
import com.example.demo.domain.model.CopilotQuery;
import com.example.demo.domain.model.VectorSearchResult;
import com.example.demo.domain.model.VectorizedPage;
import com.example.demo.domain.model.McpQueryResponse;
import com.example.demo.domain.model.McpAnswerResponse;
import com.example.demo.aplication.port.VectorSearchRepository;
import com.example.demo.aplication.usecase.GenerateAnswerUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/copilot/pages")
public class CopilotController {

    private final FindAllPagesUseCase findAllPagesUseCase;
    private final VectorizePagesUseCase vectorizePagesUseCase;
    private final SearchSimilarPagesUseCase searchSimilarPagesUseCase;
    private final VectorSearchRepository vectorSearchRepository;
    private final GenerateAnswerUseCase generateAnswerUseCase;

    public CopilotController(
        FindAllPagesUseCase findAllPagesUseCase,
        VectorizePagesUseCase vectorizePagesUseCase,
        SearchSimilarPagesUseCase searchSimilarPagesUseCase,
        VectorSearchRepository vectorSearchRepository,
        GenerateAnswerUseCase generateAnswerUseCase
    ) {
        this.findAllPagesUseCase = findAllPagesUseCase;
        this.vectorizePagesUseCase = vectorizePagesUseCase;
        this.searchSimilarPagesUseCase = searchSimilarPagesUseCase;
        this.vectorSearchRepository = vectorSearchRepository;
        this.generateAnswerUseCase = generateAnswerUseCase;
    }

    @GetMapping
    public List<ConfluencePage> getAllPages() {
        return findAllPagesUseCase.execute();
    }

    @GetMapping("/vectorized-pages")
    public List<VectorizedPage> getVectorizedPages() {
        return vectorizePagesUseCase.execute();
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpQueryResponse> query(@RequestBody CopilotQuery query) {
        if (query == null || query.getQuestion() == null || query.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<VectorSearchResult> results = searchSimilarPagesUseCase.execute(query.getQuestion());
        return ResponseEntity.ok(new McpQueryResponse(query.getQuestion(), results));
    }

    @GetMapping("/raw")
    public List<VectorizedPage> raw(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return vectorSearchRepository.scroll(limit);
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics() {
        long count = vectorSearchRepository.count();
        List<VectorizedPage> sample = vectorSearchRepository.scroll(5);
        List<String> sampleIds = sample.stream().map(VectorizedPage::getId).collect(Collectors.toList());
        return Map.of(
                "count", count,
                "sampleIds", sampleIds
        );
    }

    @PostMapping(value = "/answer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpAnswerResponse> answer(@RequestBody CopilotQuery query) {
        if (query == null || query.getQuestion() == null || query.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        McpAnswerResponse resp = generateAnswerUseCase.execute(query.getQuestion());
        return ResponseEntity.ok(resp);
    }
}