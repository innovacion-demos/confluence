package ia.confluence.back.controller;

import ia.confluence.back.service.ChatService;
import ia.confluence.back.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class RagController {

  private final IngestionService ingestionService;
  private final ChatService chatService;

  public RagController(IngestionService ingestionService, ChatService chatService) {
    this.ingestionService = ingestionService;
    this.chatService = chatService;
  }

  // Endpoint para forzar la lectura de Confluence y actualización de Qdrant
  @PostMapping("/ingest")
  public Mono<String> triggerIngestion() {
    return ingestionService.ingestData().thenReturn("Ingesta iniciada correctamente.");
  }

  // Endpoint para chatear con tus documentos de forma reactiva (streaming)
  @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> chat(@RequestBody String question, WebSession session) {
    session.getAttributes().put("conversationId", session.getId());
    final var conversationId = session.getId();
    return chatService.ask(conversationId, question);
  }
}
