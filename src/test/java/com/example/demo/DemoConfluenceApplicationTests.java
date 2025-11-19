package com.example.demo;

import com.example.demo.service.ingest.DocumentFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.vectorstore.VectorStore;
import com.example.demo.service.chat.HtmlCleaner;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class DemoConfluenceApplicationTests {

    @MockBean
    private DocumentFetcher documentFetcher;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private HtmlCleaner htmlCleaner;

    @Test
    void contextLoads() {}
}