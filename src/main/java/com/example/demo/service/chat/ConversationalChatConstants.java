package com.example.demo.service.chat;

import java.util.Set;
import org.springframework.ai.chat.prompt.PromptTemplate;

public class ConversationalChatConstants {
  public static final String PROMPT_TEMPLATE =
      """
                  Usa la siguiente información de contexto para responder a la pregunta.
                  Si la respuesta no se encuentra en el contexto, di que no tienes información al respecto.

                  CONTEXTO:
                  {context}

                  HISTORIAL:
                  {history}

                  PREGUNTA:
                  {query}
                  """;
  public static final String CONDENSE_QUERY_PROMPT =
      """
                  Reescribe la siguiente pregunta del usuario para que sea autónoma y específica, usando el historial como contexto.
                  Si la pregunta ya es autónoma, devuélvela tal cual.
                  No añadas explicaciones ni preámbulos: devuelve solo la pregunta reescrita.

                  HISTORIAL:
                  {history}

                  PREGUNTA:
                  {query}
                  """;
  // Placeholders para PromptTemplate reutilizable
  public static final String PLACEHOLDER_CONTEXT = "context";
  public static final String PLACEHOLDER_HISTORY = "history";
  public static final String PLACEHOLDER_QUERY = "query";
  // Instancias reutilizables de PromptTemplate
  public static final PromptTemplate ANSWER_PROMPT_TEMPLATE = new PromptTemplate(PROMPT_TEMPLATE);
  public static final PromptTemplate CONDENSE_QUERY_PROMPT_TEMPLATE =
      new PromptTemplate(CONDENSE_QUERY_PROMPT);
  // --- Sanitización y seguridad de entrada ---
  public static final String PLACEHOLDER_CONTEXT_TOKEN = "{context}";
  public static final String PLACEHOLDER_HISTORY_TOKEN = "{history}";
  public static final String PLACEHOLDER_QUERY_TOKEN = "{query}";
  public static final Set<String> SHORT_SKIP_WORDS =
      Set.of(
          "qué", "que", "como", "donde", "dónde", "por qué", "por que", "porque", "cuando",
          "cuándo", "quién", "quienes", "cual", "cuál");
  public static final Set<String> PRONOUNS_AMBIGUOUS =
      Set.of("eso", "esta", "este", "aquello", "allí", "ahí", "aquí");
  public static final int MIN_WORDS_FOR_REWRITE = 3;
  public static final int MIN_CHARS_FOR_REWRITE = 15;

  private ConversationalChatConstants() {
    // Private constructor to prevent instantiation
  }
}
