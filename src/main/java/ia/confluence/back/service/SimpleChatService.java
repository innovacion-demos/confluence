package ia.confluence.back.service;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class SimpleChatService implements ChatService {

  public static final String PROMPT_TEMPLATE =
      """
          Usa la siguiente información de contexto para responder a la pregunta.
          Si la respuesta no se encuentra en el contexto, di que no tienes información al respecto.

          CONTEXTO:
          {context}

          PREGUNTA:
          {query}
          """;
  private final VectorStore vectorStore;
  private final OllamaChatModel ollamaChatModel; // Instancia local de OllamaChatModel.

  /**
   * El constructor es el punto clave de la inyección de dependencias.
   *
   * @param vectorStore El bean de VectorStore (QdrantVectorStore) que Spring AI configura.
   * @param ollamaChatModel Bean de tipo OllamaChatModel. Spring lo crea y configura automáticamente
   *     para que apunte a tu servidor Ollama.
   */
  public SimpleChatService(VectorStore vectorStore, OllamaChatModel ollamaChatModel) {
    this.vectorStore = vectorStore;
    this.ollamaChatModel = ollamaChatModel;
  }

  @Override
  public Flux<String> ask(String conversationId, String query) {
    return Flux.defer(
            () -> {
              // 1. Búsqueda semántica en Qdrant
              final var searchRequest = SearchRequest.builder().query(query).build();
              final var similarDocs = vectorStore.similaritySearch(searchRequest);
              final var context =
                  similarDocs.stream()
                      .map(Document::getText)
                      .collect(Collectors.joining(System.lineSeparator()));

              // 2. Creación del Prompt
              final var promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
              final var prompt = promptTemplate.create(Map.of("context", context, "query", query));

              // 3. Llamada al modelo (Spring AI se encarga de enviarlo a Ollama)
              // Simulación de streaming: se puede dividir la respuesta en partes y emitirlas con
              // Flux
              final var response = ollamaChatModel.call(prompt).getResult().getOutput().getText();
              if (response == null || response.isBlank()) {
                return Flux.empty();
              }
              return Flux.just(response);
            })
        .subscribeOn(Schedulers.boundedElastic());
  }
}
