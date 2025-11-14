package com.example.demo.aplication.usecase;

import com.example.demo.aplication.port.ContentFetcherPort;
import com.example.demo.domain.model.ConfluencePage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FindAllPagesUseCase {

    private final ContentFetcherPort contentFetcherPort;

    public FindAllPagesUseCase(ContentFetcherPort contentFetcherPort) {
        this.contentFetcherPort = contentFetcherPort;
    }

    public List<ConfluencePage> execute() {
        return contentFetcherPort.fetchAllPages();
    }
}