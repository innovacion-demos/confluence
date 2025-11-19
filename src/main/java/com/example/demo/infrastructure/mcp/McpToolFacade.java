package com.example.demo.infrastructure.mcp;

import com.example.demo.infrastructure.mcp.PageInfo;
import com.example.demo.service.chat.ConversationalChatService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class McpToolFacade {
    private final ConversationalChatService chatService;
    private final com.example.demo.service.ingest.IngestionService ingestionService;

    public McpToolFacade(ConversationalChatService chatService, com.example.demo.service.ingest.IngestionService ingestionService) {
        this.chatService = chatService;
        this.ingestionService = ingestionService;
    }

    public List<PageInfo> listPages(int limit) {
        var request = org.springframework.ai.vectorstore.SearchRequest.builder()
            .query("")
            .topK(limit)
            .build();
        var docs = chatService.getVectorStore().similaritySearch(request);
        return docs.stream()
            .map(doc -> new PageInfo(
                doc.getId(),
                (String) doc.getMetadata().getOrDefault("title", ""),
                (String) doc.getMetadata().getOrDefault("url", ""),
                doc.getText(),
                null
            ))
            .collect(Collectors.toList());
    }

    public List<PageInfo> searchSimilar(String query, int limit) {
        var request = org.springframework.ai.vectorstore.SearchRequest.builder()
            .query(query)
            .topK(limit)
            .build();
        var docs = chatService.getVectorStore().similaritySearch(request);
        return docs.stream()
            .map(doc -> new PageInfo(
                doc.getId(),
                (String) doc.getMetadata().getOrDefault("title", ""),
                (String) doc.getMetadata().getOrDefault("url", ""),
                doc.getText(),
                null // No score disponible
            ))
            .collect(Collectors.toList());
    }

    public String ingestConfluence() {
        try {
            ingestionService.ingestData().subscribe();
            return "Ingesta lanzada correctamente";
        } catch (Exception e) {
            return "Error en la ingesta: " + e.getMessage();
        }
    }
}
