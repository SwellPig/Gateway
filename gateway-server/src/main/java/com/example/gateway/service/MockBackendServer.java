package com.example.gateway.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Instant;

@Component
public class MockBackendServer {
    private DisposableServer server;

    @PostConstruct
    public void start() {
        server = HttpServer.create()
                .port(9001)
                .route(routes -> routes
                        .get("/mock/account/{id}", (req, res) ->
                                res.header("Content-Type", "application/json")
                                        .sendString(reactor.core.publisher.Mono.just(
                                                "{\"id\":\"" + req.param("id") + "\",\"balance\":1000,\"ts\":\"" + Instant.now() + "\"}")))
                        .post("/mock/transfer", (req, res) ->
                                res.header("Content-Type", "application/json")
                                        .sendString(reactor.core.publisher.Mono.just(
                                                "{\"status\":\"accepted\",\"ts\":\"" + Instant.now() + "\"}")))
                        .get("/mock/health", (req, res) ->
                                res.header("Content-Type", "application/json")
                                        .sendString(reactor.core.publisher.Mono.just("{\"ok\":true}"))))
                .bindNow();
    }
}
