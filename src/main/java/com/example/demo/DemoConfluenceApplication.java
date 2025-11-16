package com.example.demo;

import com.example.demo.config.ConfluenceProperties;
import com.example.demo.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ConfluenceProperties.class, RagProperties.class})
public class DemoConfluenceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemoConfluenceApplication.class, args);
  }
}
