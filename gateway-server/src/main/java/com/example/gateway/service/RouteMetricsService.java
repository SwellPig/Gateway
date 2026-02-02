package com.example.gateway.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RouteMetricsService {
    private final ConcurrentHashMap<String, Metric> metrics = new ConcurrentHashMap<>();

    public void recordHit(String routeId) {
        Metric metric = metrics.computeIfAbsent(routeId, id -> new Metric());
        metric.count.incrementAndGet();
        metric.lastHit.set(System.currentTimeMillis());
    }

    public List<RouteMetricView> snapshot() {
        List<RouteMetricView> views = new ArrayList<>();
        metrics.forEach((id, metric) -> views.add(new RouteMetricView(
                id,
                metric.count.get(),
                metric.lastHit.get() == 0 ? null : Instant.ofEpochMilli(metric.lastHit.get()).toString()
        )));
        return views;
    }

    private static final class Metric {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong lastHit = new AtomicLong();
    }

    public record RouteMetricView(String routeId, long hits, String lastHit) {
    }
}
