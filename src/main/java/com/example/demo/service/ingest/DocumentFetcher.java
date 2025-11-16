package com.example.demo.service.ingest;

import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentFetcher {
  Mono<List<String>> fetchDocuments();
}
