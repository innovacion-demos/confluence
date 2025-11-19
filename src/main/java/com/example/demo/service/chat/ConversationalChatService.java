package com.example.demo.service.chat;

import static com.example.demo.service.chat.ConversationalChatConstants.*;

import com.example.demo.config.RagProperties;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@AllArgsConstructor
public class ConversationalChatService implements ChatService {
  private static final Logger log = LoggerFactory.getLogger(ConversationalChatService.class);

  private final VectorStore vectorStore;
  private final OllamaChatModel ollamaChatModel;
  private final ChatMemory chatMemory;
  private final RagProperties ragProperties;

  @Override
  public Flux<String> ask(String conversationId, String query) {
    return Flux.defer(
            () -> {
              final var sanitizedQuery = sanitizeUserInput(query);
              if (!Objects.equals(query, sanitizedQuery)) {
                log.debug("Query sanitizada: '{}' -> '{}'", query, sanitizedQuery);
              }
              addUserMessage(conversationId, sanitizedQuery);
              final var trimmed = getHistoryMessages(conversationId);
              final var history = formatHistoryForPrompt(trimmed);
              final var effectiveQuery = rewriteQueryIfNeeded(history, sanitizedQuery);
              logQueryRewrite(sanitizedQuery, effectiveQuery);
              final var searchRequest = buildSearchRequest(effectiveQuery);
              final var context = resolveContext(searchRequest);
              final var prompt = buildAnswerPrompt(context, history, sanitizedQuery);
              log.info(prompt.toString());
              final var responseOpt = callModelForText(prompt);
              responseOpt.ifPresent(r -> persistAssistantMessage(conversationId, r));
              return responseOpt.map(Flux::just).orElseGet(Flux::empty);
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private void addUserMessage(final String conversationId, final String query) {
    chatMemory.add(conversationId, List.of(new UserMessage(query)));
  }

  private List<Message> getHistoryMessages(final String conversationId) {
    final var historyMessages = chatMemory.get(conversationId);
    return trimHistoryMessages(historyMessages);
  }

  private String formatHistoryForPrompt(final List<Message> trimmed) {
    return formatHistory(trimmed);
  }

  private String rewriteQueryIfNeeded(final String history, final String originalQuery) {
    return buildSearchQuery(history, originalQuery);
  }

  private void logQueryRewrite(final String original, final String rewritten) {
    if (!Objects.equals(original, rewritten)) {
      log.info("Query reescrita para búsqueda: '{}' -> '{}'", original, rewritten);
    }
  }

  private SearchRequest buildSearchRequest(final String searchQuery) {
    return SearchRequest.builder()
        .query(searchQuery)
        .topK(1)
        .similarityThreshold(ragProperties.getSimilarityThreshold())
        .build();
  }

  private String resolveContext(final SearchRequest request) {
    final var similarDocs = vectorStore.similaritySearch(request);
    return similarDocs.stream().findFirst().map(Document::getText).orElse("");
  }

  private org.springframework.ai.chat.prompt.Prompt buildAnswerPrompt(
      final String context, final String history, final String originalQuery) {
    return ANSWER_PROMPT_TEMPLATE.create(
        Map.of(
            PLACEHOLDER_CONTEXT, context.isBlank() ? "(Sin contexto relevante)" : context,
            PLACEHOLDER_HISTORY, history.isBlank() ? "(Sin historial)" : history,
            PLACEHOLDER_QUERY, originalQuery));
  }

  private Optional<String> callModelForText(
      final org.springframework.ai.chat.prompt.Prompt prompt) {
    final var response = ollamaChatModel.call(prompt).getResult().getOutput().getText();
    if (response == null || response.isBlank()) return Optional.empty();
    return Optional.of(response);
  }

  private void persistAssistantMessage(final String conversationId, final String response) {
    chatMemory.add(conversationId, List.of(new AssistantMessage(response)));
  }

  // Recorta la lista de mensajes a los últimos N relevantes (usuario y asistente)
  private List<Message> trimHistoryMessages(List<Message> messages) {
    if (messages == null || messages.isEmpty()) return List.of();
    final var relevant =
        messages.stream()
            .filter(
                m -> {
                  m.getMessageType();
                  return m.getText() != null && !m.getText().isBlank();
                })
            .filter(
                m -> {
                  final var type = m.getMessageType().name();
                  return "USER".equals(type) || "ASSISTANT".equals(type);
                })
            .toList();
    final var start = Math.max(0, relevant.size() - ragProperties.getMaxHistoryMessages());
    return relevant.subList(start, relevant.size());
  }

  private String buildSearchQuery(String history, String query) {
    final var sanitized = sanitizeUserInput(query);
    if (!shouldRewriteQuery(history, sanitized)) return sanitized;
    final var trimmedHistory = truncateHistoryForRewrite(history);
    try {
      final var prompt =
          CONDENSE_QUERY_PROMPT_TEMPLATE.create(
              Map.of(PLACEHOLDER_HISTORY, trimmedHistory, PLACEHOLDER_QUERY, sanitized));
      final var modelOut = ollamaChatModel.call(prompt).getResult().getOutput().getText();
      return postProcessModelRewritten(modelOut, sanitized);
    } catch (Exception e) {
      log.warn("Fallo reescribiendo la query; uso la original sanitizada", e);
      return sanitized;
    }
  }

  private String formatHistory(List<Message> messages) {
    if (messages == null || messages.isEmpty()) {
      return "";
    }
    return messages.stream()
        .map(m -> m.getMessageType() + ": " + sanitize(m.getText()))
        .collect(Collectors.joining(System.lineSeparator()));
  }

  private String sanitize(Object content) {
    if (content == null) return "";
    return content.toString().trim();
  }

  // --- Métodos de sanitización y heurísticas ---
  private String sanitizeUserInput(final String raw) {
    if (raw == null) return "";
    var s = raw.trim();
    s = removeControlChars(s);
    s = normalizeWhitespace(s);
    s = escapeConflictTokens(s);
    s = limitQueryLength(s);
    return s;
  }

  private String removeControlChars(final String s) {
    // Elimina caracteres de control excepto saltos de línea que luego normalizamos
    return s.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
  }

  private String normalizeWhitespace(final String s) {
    return s.replaceAll("\\s+", " ").trim();
  }

  private String escapeConflictTokens(final String s) {
    var out = s;
    // Sustituye tokens de placeholders para evitar inyección directa en el prompt
    out = out.replace(PLACEHOLDER_CONTEXT_TOKEN, "(context)");
    out = out.replace(PLACEHOLDER_HISTORY_TOKEN, "(history)");
    out = out.replace(PLACEHOLDER_QUERY_TOKEN, "(query)");
    // Escapa llaves sueltas
    out = out.replace("{", "(").replace("}", ")");
    return out;
  }

  private String limitQueryLength(final String s) {
    final var max = ragProperties.getMaxQueryChars();
    if (s.length() <= max) return s;
    // Conserva la parte final (más reciente) si es muy larga
    return s.substring(s.length() - max);
  }

  private boolean shouldRewriteQuery(final String history, final String query) {
    if (query == null || query.isBlank()) return false;
    final var words = Arrays.stream(query.split("\\s+")).filter(w -> !w.isBlank()).toList();
    if (words.size() < MIN_WORDS_FOR_REWRITE) return false;
    if (query.length() < MIN_CHARS_FOR_REWRITE) return false;
    final var lowerWords = words.stream().map(w -> w.toLowerCase(Locale.ROOT)).toList();
    final var allShortSkip = SHORT_SKIP_WORDS.containsAll(lowerWords); // reemplaza stream allMatch
    if (allShortSkip) return false;
    final var lower = query.toLowerCase(Locale.ROOT);
    final var containsPronoun = PRONOUNS_AMBIGUOUS.stream().anyMatch(lower::contains);
    return containsPronoun || (history != null && !history.isBlank());
  }

  private String truncateHistoryForRewrite(final String history) {
    if (history == null) return "";
    final var max = ragProperties.getMaxHistoryChars();
    if (history.length() <= max) return history;
    return history.substring(history.length() - max);
  }

  private String postProcessModelRewritten(final String out, final String original) {
    if (out == null) return original;
    var s = out.trim();
    if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
      s = s.substring(1, s.length() - 1).trim();
    }
    s = normalizeWhitespace(s);
    s = escapeConflictTokens(s);
    if (s.isEmpty()) return original;
    return s;
  }

  public VectorStore getVectorStore() {
    return vectorStore;
  }
}
