package com.example.demo.aplication.port;
import java.util.List;
public interface EmbeddingRepository {
    List<Double> vectorize(String text);
}
