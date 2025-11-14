package com.example.demo.aplication.port;
import com.example.demo.domain.model.VectorSearchResult;
import com.example.demo.domain.model.VectorizedPage;

import java.util.List;

public interface VectorSearchRepository {
    void save(VectorizedPage page);

    List<VectorSearchResult> searchSimilar(List<Double> queryVector);

    List<VectorizedPage> scroll(int limit);

    long count();
}
