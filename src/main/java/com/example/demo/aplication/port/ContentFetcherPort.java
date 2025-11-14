package com.example.demo.aplication.port;


import com.example.demo.domain.model.ConfluencePage;

import java.util.List;
import java.util.Optional;

public interface ContentFetcherPort {
    List<ConfluencePage> fetchAllPages();
    Optional<ConfluencePage> fetchPageById(String id);
}

