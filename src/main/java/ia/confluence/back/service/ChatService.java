package ia.confluence.back.service;

import reactor.core.publisher.Flux;

public interface ChatService {
  Flux<String> ask(String conversationId, String query);
}
