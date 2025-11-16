package ia.confluence.back.infra;

import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentFetcher {
  Mono<List<String>> fetchDocuments();
}
