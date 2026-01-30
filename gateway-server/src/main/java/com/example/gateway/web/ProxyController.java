package com.example.gateway.web;

import com.example.gateway.model.RouteRule;
import com.example.gateway.model.RuleSnapshot;
import com.example.gateway.service.RouteMatcher;
import com.example.gateway.service.RuleService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@RestController
public class ProxyController {
    private final WebClient webClient;
    private final RuleService ruleService;
    private final RouteMatcher matcher;

    public ProxyController(WebClient webClient, RuleService ruleService, RouteMatcher matcher) {
        this.webClient = webClient;
        this.ruleService = ruleService;
        this.matcher = matcher;
    }

    @RequestMapping("/**")
    public Mono<Void> proxy(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/admin") || path.equals("/index.html") || path.equals("/styles.css")
                || path.equals("/app.js") || path.equals("/favicon.ico") || path.equals("/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "GET";
        RouteRule route = matchRoute(path, method);
        if (route == null) {
            return writeJson(exchange.getResponse(), HttpStatus.NOT_FOUND, "{\"error\":\"未匹配到路由规则\"}");
        }

        String forwardPath = applyRewrite(route, path);
        URI target = buildTargetUri(route.getTarget(), forwardPath, exchange.getRequest().getURI().getRawQuery());
        ServerHttpRequest request = exchange.getRequest();

        return webClient.method(request.getMethod())
                .uri(target)
                .headers(headers -> copyHeaders(request.getHeaders(), headers))
                .body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(clientResponse -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(clientResponse.statusCode());
                    response.getHeaders().putAll(clientResponse.headers().asHttpHeaders());
                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                })
                .onErrorResume(error ->
                        writeJson(exchange.getResponse(), HttpStatus.BAD_GATEWAY,
                                "{\"error\":\"转发失败\",\"detail\":\"" + error.getMessage() + "\"}"));
    }

    private RouteRule matchRoute(String path, String method) {
        RuleSnapshot snapshot = ruleService.getSnapshot();
        for (RouteRule route : snapshot.getRoutes()) {
            if (route.getMethods() != null && !route.getMethods().isEmpty()) {
                String upper = method.toUpperCase(Locale.ROOT);
                List<String> methods = route.getMethods().stream()
                        .map(item -> item.toUpperCase(Locale.ROOT))
                        .toList();
                if (!methods.contains(upper)) {
                    continue;
                }
            }
            if (matcher.matches(route.getPath(), path)) {
                return route;
            }
        }
        return null;
    }

    private String applyRewrite(RouteRule route, String path) {
        if (StringUtils.hasText(route.getRewrite())) {
            String regex = matcher.toRegex(route.getPath());
            return path.replaceAll("^" + regex + "$", route.getRewrite());
        }
        if (route.getStripPrefix() != null && route.getStripPrefix() > 0) {
            String[] parts = StringUtils.tokenizeToStringArray(path, "/");
            if (route.getStripPrefix() >= parts.length) {
                return "/";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = route.getStripPrefix(); i < parts.length; i++) {
                builder.append("/").append(parts[i]);
            }
            return builder.toString();
        }
        return path;
    }

    private URI buildTargetUri(String target, String path, String query) {
        String base = target.endsWith("/") ? target.substring(0, target.length() - 1) : target;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String uri = base + normalizedPath;
        if (StringUtils.hasText(query)) {
            uri += "?" + query;
        }
        return URI.create(uri);
    }

    private void copyHeaders(HttpHeaders source, HttpHeaders target) {
        target.addAll(source);
        target.remove(HttpHeaders.HOST);
    }

    private Mono<Void> writeJson(ServerHttpResponse response, HttpStatus status, String body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
