package ia.confluence.back.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
  /** Umbral de similitud mínimo para considerar un documento relevante. */
  private double similarityThreshold = 0.5d;

  /** Máximo de caracteres conservados del historial para prompts de reescritura. */
  private int maxHistoryChars = 3000;

  /** Máximo de mensajes del historial considerados. */
  private int maxHistoryMessages = 20;

  /** Máximo de caracteres permitidos en la query del usuario. */
  private int maxQueryChars = 500;

  /** Número máximo de documentos a usar como contexto. */
  private int maxContextDocs = 3;

  /** Máximo de caracteres para el contexto final unido. */
  private int maxContextChars = 3500;

  /** Separador entre segmentos de contexto. */
  private String contextJoinSeparator = "\n\n---\n\n";

  /** Deduplicar documentos por fuente/url. */
  private boolean deduplicateBySource = true;
}
