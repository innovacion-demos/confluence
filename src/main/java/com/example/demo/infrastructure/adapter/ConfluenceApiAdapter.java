package com.example.demo.infrastructure.adapter;

import com.example.demo.aplication.port.ContentFetcherPort;
import com.example.demo.domain.model.ConfluencePage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ConfluenceApiAdapter implements ContentFetcherPort {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String spaceKey;

    public ConfluenceApiAdapter(
            RestTemplateBuilder builder,
            @Value("${confluence.email}") String email,
            @Value("${confluence.token}") String token,
            @Value("${confluence.baseUrl}") String baseUrl,
            @Value("${confluence.spaceKey}") String spaceKey
    ) {
        this.restTemplate = builder
                .basicAuthentication(email, token)
                .build();
        this.baseUrl = baseUrl;
        this.spaceKey = spaceKey;
    }

    @Override
    public List<ConfluencePage> fetchAllPages() {
        String url = baseUrl + "?spaceKey=" + spaceKey + "&expand=body.view";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        List<ConfluencePage> pages = new ArrayList<>();
        for (JsonNode result : response.getBody().get("results")) {
            ConfluencePage page = new ConfluencePage();
            page.setId(result.get("id").asText());
            page.setTitle(result.get("title").asText());
            page.setContent(result.get("body").get("view").get("value").asText());
            page.setUrl("https://zelzeusyt2002.atlassian.net/wiki" + result.get("_links").get("webui").asText());
            pages.add(page);
        }
        return pages;
    }

    @Override
    public Optional<ConfluencePage> fetchPageById(String id) {
        String url = baseUrl + "/" + id + "?expand=body.view";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode result = response.getBody();

        ConfluencePage page = new ConfluencePage();
        page.setId(result.get("id").asText());
        page.setTitle(result.get("title").asText());
        page.setContent(result.get("body").get("view").get("value").asText());
        page.setUrl("https://zelzeusyt2002.atlassian.net/wiki" + result.get("_links").get("webui").asText());

        return Optional.of(page);
    }
}
