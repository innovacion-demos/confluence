package com.example.demo;

import py4j.GatewayServer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import com.example.demo.infrastructure.mcp.McpToolFacade;

public class Py4jSpringBootLauncher {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(DemoConfluenceApplication.class, args);
        McpToolFacade facade = ctx.getBean(McpToolFacade.class);
        GatewayServer gatewayServer = new GatewayServer(facade, 25333); // Puerto configurable
        gatewayServer.start();
        System.out.println("GatewayServer started on port 25333");
    }
}