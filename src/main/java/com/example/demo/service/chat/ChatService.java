package com.example.demo.service.chat;

import reactor.core.publisher.Flux;

public interface ChatService {
  Flux<String> ask(String conversationId, String query);
}
