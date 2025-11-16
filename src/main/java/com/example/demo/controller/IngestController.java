package com.example.demo.controller;

import com.example.demo.service.ingest.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class IngestController {
  private final IngestionService ingestionService;

  @PostMapping("/ingest")
  public Mono<String> triggerIngestion() {
    return ingestionService.ingestData().thenReturn("Ingesta iniciada correctamente.");
  }
}
