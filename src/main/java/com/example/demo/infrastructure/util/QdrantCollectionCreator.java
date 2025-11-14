package com.example.demo.infrastructure.util;


import org.springframework.http.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class QdrantCollectionCreator {
    public static void main(String[] args) {
        String baseUrl = "https://584009a3-3ac8-4b91-8d2f-fb14d22d290e.eu-central-1-0.aws.cloud.qdrant.io:6333";
        String apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2Nlc3MiOiJtIn0.6sfhg9r-5zk8f2U8L_wdYlS-5nd-7Hv7ZUW4XGro2oI"; // Nuevo API Key generado con permisos globales

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        String jsonBody = "{\n" +
                "  \"vectors\": {\n" +
                "    \"size\": 100,\n" +
                "    \"distance\": \"Cosine\"\n" +
                "  }\n" +
                "}";

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/collections/knowpilot",
                    HttpMethod.PUT,
                    request,
                    String.class
            );
            System.out.println("Respuesta: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error al crear la colección: " + e.getMessage());
        }
    }
}
