package com.example.demo.infrastructure.adapter;

import com.example.demo.aplication.port.VectorSearchRepository;
import com.example.demo.domain.model.VectorSearchResult;
import com.example.demo.domain.model.VectorizedPage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class QdrantVectorSearchAdapter implements VectorSearchRepository {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private static final Logger log = LoggerFactory.getLogger(QdrantVectorSearchAdapter.class);
    private static final int EXPECTED_DIMENSION = 100; // podría externalizarse en properties

    public QdrantVectorSearchAdapter(
            @Value("${qdrant.baseUrl}") String baseUrl,
            @Value("${qdrant.apiKey}") String apiKey
    ) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void save(VectorizedPage page) {
        try {
            if (page.getVector() == null || page.getVector().isEmpty()) {
                log.warn("Vector vacío para página {} - se omite", page.getId());
                return;
            }
            // Normalizar dimensión
            List<Double> vec = page.getVector();
            if (vec.size() != EXPECTED_DIMENSION) {
                log.warn("Vector dimensión {} distinta a {} para página {}. Ajustando.", vec.size(), EXPECTED_DIMENSION, page.getId());
                List<Double> adjusted = new ArrayList<>(vec);
                if (adjusted.size() > EXPECTED_DIMENSION) {
                    adjusted = adjusted.subList(0, EXPECTED_DIMENSION);
                } else {
                    while (adjusted.size() < EXPECTED_DIMENSION) {
                        adjusted.add(0.0); // padding con ceros
                    }
                }
                vec = adjusted;
            }
            // Sanitizar id: si es dígitos -> número, si es UUID válido -> string, else generar UUID
            Object pointId;
            String rawId = page.getId();
            if (rawId != null && rawId.matches("\\d+")) {
                try {
                    pointId = Long.parseLong(rawId); // Qdrant acepta entero sin signo
                } catch (NumberFormatException nfe) {
                    pointId = UUID.randomUUID().toString();
                }
            } else if (rawId != null && rawId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                pointId = rawId;
            } else {
                pointId = UUID.randomUUID().toString();
            }

            String cleanedContent = page.getContent() == null ? "" : page.getContent().replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> payload = Map.of(
                    "points", List.of(Map.of(
                            "id", pointId,
                            "vector", vec,
                            "payload", Map.of(
                                    "title", page.getTitle(),
                                    "url", page.getUrl(),
                                    "content", cleanedContent
                            )
                    ))
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> resp = restTemplate.exchange(baseUrl + "/collections/knowpilot/points", HttpMethod.PUT, request, String.class);
            log.info("Insertado punto página {} (id enviado {}) status {}", page.getId(), pointId, resp.getStatusCode());
        } catch (Exception e) {
            log.error("Error guardando vector en Qdrant página {}: {}", page.getId(), e.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> searchSimilar(List<Double> queryVector) {
        List<VectorSearchResult> results = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            Map<String, Object> payload = Map.of("vector", queryVector, "limit", 5, "with_payload", true);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/collections/knowpilot/points/search", request, JsonNode.class);
            if (response.getBody() == null || response.getBody().get("result") == null) { log.warn("Respuesta de búsqueda sin resultados"); return results; }
            for (JsonNode result : response.getBody().get("result")) {
                JsonNode payloadNode = result.get("payload");
                String title = payloadNode != null && payloadNode.has("title") ? payloadNode.get("title").asText() : null;
                String url = payloadNode != null && payloadNode.has("url") ? payloadNode.get("url").asText() : null;
                String content = payloadNode != null && payloadNode.has("content") ? payloadNode.get("content").asText() : null;
                double score = result.get("score").asDouble();
                results.add(new VectorSearchResult(result.get("id").asText(), title, url, content, score));
            }
        } catch (Exception e) { log.error("Error buscando similares en Qdrant: {}", e.getMessage()); }
        return results;
    }

    @Override
    public List<VectorizedPage> scroll(int limit) {
        List<VectorizedPage> pages = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            Map<String, Object> payload = Map.of(
                    "limit", limit,
                    "with_payload", true,
                    "with_vectors", false
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    baseUrl + "/collections/knowpilot/points/scroll",
                    request,
                    JsonNode.class
            );
            JsonNode result = response.getBody().get("result");
            if (result != null && result.get("points") != null) {
                for (JsonNode p : result.get("points")) {
                    JsonNode payloadNode = p.get("payload");
                    VectorizedPage vp = new VectorizedPage();
                    vp.setId(p.get("id").asText());
                    if (payloadNode != null) {
                        vp.setTitle(payloadNode.has("title") ? payloadNode.get("title").asText() : null);
                        vp.setUrl(payloadNode.has("url") ? payloadNode.get("url").asText() : null);
                    }
                    pages.add(vp);
                }
            }
        } catch (Exception e) {
            log.error("Error en scroll Qdrant: {}", e.getMessage());
        }
        return pages;
    }

    @Override
    public long count() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            Map<String, Object> payload = Map.of("filter", null);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    baseUrl + "/collections/knowpilot/points/count",
                    request,
                    JsonNode.class
            );
            JsonNode result = response.getBody().get("result");
            if (result != null && result.has("count")) {
                return result.get("count").asLong();
            }
        } catch (Exception e) {
            log.error("Error obteniendo count Qdrant: {}", e.getMessage());
        }
        return 0L;
    }
}
