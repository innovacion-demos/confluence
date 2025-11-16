package com.example.demo.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {
  /**
   * Crea un bean singleton de ChatMemory usando MessageWindowChatMemory. Maneja múltiples
   * conversaciones mediante conversationId.
   */
  @Bean
  public ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder().maxMessages(20).build();
  }
}
