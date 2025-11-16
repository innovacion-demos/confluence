package com.example.demo.controller;

import com.example.demo.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatController {
  private final ChatService chatService;

  @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> chat(@RequestBody String question, WebSession session) {
    session.getAttributes().put("conversationId", session.getId());

    final var conversationId = session.getId();

    return chatService.ask(conversationId, question);
  }
}
