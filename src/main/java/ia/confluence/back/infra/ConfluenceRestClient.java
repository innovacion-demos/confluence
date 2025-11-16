package ia.confluence.back.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import ia.confluence.back.config.ConfluenceProperties;
import java.util.Base64;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// Importa clases de Jackson (JsonNode) para parsear la respuesta rápida

@Service
public class ConfluenceRestClient implements DocumentFetcher {

  private final WebClient webClient;
  private final ConfluenceProperties props;

  public ConfluenceRestClient(ConfluenceProperties props, WebClient.Builder builder) {
    this.props = props;
    final var auth = props.username() + ":" + props.apiToken();
    final var encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

    this.webClient =
        builder
            .baseUrl(props.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build();
  }

  @Override
  public Mono<List<String>> fetchDocuments() {
    // Realiza la petición HTTP de forma reactiva
    return webClient
        .get()
        .uri("/rest/api/content?spaceKey=" + props.spaceKey() + "&expand=body.storage&limit=10")
        .retrieve()
        .bodyToMono(String.class)
        .map(
            response -> {
              final var mapper = new ObjectMapper();
              try {
                final var root = mapper.readTree(response);
                final var results = root.path("results");
                if (!results.isArray()) return List.of();
                return StreamSupport.stream(results.spliterator(), false)
                    .map(node -> node.path("body").path("storage").path("value").asText())
                    .filter(text -> !text.isEmpty())
                    .toList();
              } catch (Exception e) {
                return List.of();
              }
            });
  }
}
