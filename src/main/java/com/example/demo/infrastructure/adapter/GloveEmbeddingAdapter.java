package com.example.demo.infrastructure.adapter;

import com.example.demo.aplication.port.EmbeddingRepository;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GloveEmbeddingAdapter implements EmbeddingRepository {
    private final Map<String, List<Double>> embeddings = new HashMap<>();
    private static final int DIM = 100;
    private static final double RANGE = 0.1; // reducido
    private static final Logger log = LoggerFactory.getLogger(GloveEmbeddingAdapter.class);

    public GloveEmbeddingAdapter() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/glove.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                String word = parts[0];
                List<Double> vector = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    vector.add(Double.parseDouble(parts[i]));
                }
                embeddings.put(word, vector);
            }
        }
    }

    private List<Double> unknownVector(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            List<Double> vec = new ArrayList<>(DIM);
            for (int i = 0; i < DIM; i++) {
                int b = hash[i % hash.length] & 0xFF;
                double v = ((b / 255.0) - 0.5) * 2 * RANGE; // distribuye en [-RANGE,RANGE]
                vec.add(v);
            }
            return vec;
        } catch (Exception e) {
            List<Double> zeros = new ArrayList<>(Collections.nCopies(DIM, 0.0));
            return zeros;
        }
    }

    @Override
    public List<Double> vectorize(String text) {
        String cleaned = text.toLowerCase().replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ ]", " ");
        cleaned = cleaned.replaceAll("[áÁ]", "a").replaceAll("[éÉ]", "e").replaceAll("[íÍ]", "i")
                .replaceAll("[óÓ]", "o").replaceAll("[úÚ]", "u").replaceAll("ñ", "n");
        String[] tokens = cleaned.trim().split("\\s+");
        List<Double> acc = new ArrayList<>(Collections.nCopies(DIM, 0.0));
        int used = 0;
        for (String token : tokens) {
            if (token.isBlank()) continue;
            List<Double> vec = embeddings.get(token);
            if (vec == null) {
                // palabra desconocida: se ignora para no introducir ruido
                continue;
            }
            for (int i = 0; i < DIM && i < vec.size(); i++) {
                acc.set(i, acc.get(i) + vec.get(i));
            }
            used++;
        }
        if (used > 0) {
            for (int i = 0; i < DIM; i++) {
                acc.set(i, acc.get(i) / used);
            }
        } else {
            log.debug("Vectorización sin tokens reconocidos ({} tokens desconocidos). Devuelvo ceros.", tokens.length);
        }
        double norm = 0.0;
        for (double v : acc) norm += v * v;
        norm = Math.sqrt(norm);
        if (used > 0 && norm > 0.0) {
            for (int i = 0; i < acc.size(); i++) {
                acc.set(i, acc.get(i) / norm);
            }
            log.debug("Vectorizado normalizado L2 tokens={} usados={} norm_pre={} norm_post=1.0", tokens.length, used, String.format("%.5f", norm));
        } else {
            log.debug("Vector sin normalización (used={} norm={})", used, String.format("%.5f", norm));
        }
        return acc;
    }
}