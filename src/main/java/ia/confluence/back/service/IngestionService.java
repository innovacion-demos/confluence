package ia.confluence.back.service;

import ia.confluence.back.infra.DocumentFetcher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class IngestionService {

  private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
  private final DocumentFetcher documentFetcher;
  private final VectorStore vectorStore;
  private final HtmlCleaner htmlCleaner;

  public IngestionService(
      DocumentFetcher documentFetcher, VectorStore vectorStore, HtmlCleaner htmlCleaner) {
    this.documentFetcher = documentFetcher;
    this.vectorStore = vectorStore;
    this.htmlCleaner = htmlCleaner;
  }

  public Mono<Void> ingestData() {
    return documentFetcher
        .fetchDocuments()
        .flatMap(
            rawContents ->
                Mono.fromRunnable(
                        () -> {
                          log.info(
                              "Ingesta: recibidos {} contenidos para procesar", rawContents.size());

                          final var documents =
                              rawContents.stream()
                                  .map(this::toDocumentWithDeterministicId)
                                  // De-duplicar dentro del batch por su id/hash, conservando orden
                                  .collect(
                                      Collectors.toMap(
                                          Document::getId,
                                          Function.identity(),
                                          (d1, d2) -> d1,
                                          LinkedHashMap::new))
                                  .values()
                                  .stream()
                                  .toList();

                          vectorStore.add(documents);
                          log.info(
                              "Documentos vectorizados y guardados en Qdrant. Total añadidos/actualizados: {}",
                              documents.size());
                        })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then())
        .doOnError(e -> log.error("Error durante la ingesta de documentos", e));
  }

  private Document toDocumentWithDeterministicId(String content) {
    final var cleanText = htmlCleaner.clean(content);
    final var hash = sha256Hex(cleanText);
    // Id determinista y válido para Qdrant (UUID basado en el contenido)
    final var uuid = UUID.nameUUIDFromBytes(cleanText.getBytes(StandardCharsets.UTF_8)).toString();
    return new Document(uuid, cleanText, buildMetadata(hash));
  }

  private Map<String, Object> buildMetadata(String hash) {
    return Map.of("source", "confluence", "hash", hash);
  }

  private String sha256Hex(String text) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      // No debería ocurrir en JVM moderna; fallback a hashCode si sucede
      log.warn("Algoritmo SHA-256 no disponible, usando hashCode como fallback", e);
      return Integer.toHexString(text.hashCode());
    }
  }
}
