package ia.confluence.back.service;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class HtmlCleaner {
  /** Limpia el HTML de un texto usando Jsoup para mayor robustez. */
  public String clean(String input) {
    return Jsoup.parse(input).text();
  }
}
