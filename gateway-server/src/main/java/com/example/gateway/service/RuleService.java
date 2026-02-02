package com.example.gateway.service;

import com.example.gateway.model.RouteRule;
import com.example.gateway.model.RuleSnapshot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RuleService {
    private final RuleStorage storage;
    private final AtomicReference<RuleSnapshot> snapshotRef = new AtomicReference<>();
    private volatile long lastModified = 0L;

    public RuleService(RuleStorage storage) throws IOException {
        this.storage = storage;
        RuleSnapshot snapshot = storage.readSnapshot();
        snapshotRef.set(snapshot);
        updateLastModified();
    }

    public RuleSnapshot getSnapshot() {
        return snapshotRef.get();
    }

    public RouteRule addRoute(RouteRule route) throws IOException {
        RouteRule created = new RouteRule();
        String now = Instant.now().toString();
        created.setId(Optional.ofNullable(route.getId()).orElse(UUID.randomUUID().toString()));
        created.setPath(route.getPath());
        created.setMethods(route.getMethods());
        created.setTarget(route.getTarget());
        created.setStripPrefix(route.getStripPrefix());
        created.setRewrite(route.getRewrite());
        created.setGroup(route.getGroup());
        created.setAuthType(route.getAuthType());
        created.setApiKey(route.getApiKey());
        created.setRateLimitQps(route.getRateLimitQps());
        created.setEnabled(route.getEnabled() == null ? Boolean.TRUE : route.getEnabled());
        created.setTimeoutMs(route.getTimeoutMs());
        created.setCreatedAt(now);
        created.setUpdatedAt(now);

        RuleSnapshot current = snapshotRef.get();
        List<RouteRule> updated = new java.util.ArrayList<>(current.getRoutes());
        updated.add(created);
        RuleSnapshot next = new RuleSnapshot();
        next.setVersion(Instant.now().toString());
        next.setRoutes(updated);
        saveSnapshot(next);
        return created;
    }

    public Optional<RouteRule> updateRoute(String id, RouteRule patch) throws IOException {
        RuleSnapshot current = snapshotRef.get();
        List<RouteRule> updated = new java.util.ArrayList<>();
        RouteRule updatedRule = null;
        for (RouteRule route : current.getRoutes()) {
            if (route.getId().equals(id)) {
                String now = Instant.now().toString();
                RouteRule merged = new RouteRule();
                merged.setId(id);
                merged.setPath(Optional.ofNullable(patch.getPath()).orElse(route.getPath()));
                merged.setMethods(Optional.ofNullable(patch.getMethods()).orElse(route.getMethods()));
                merged.setTarget(Optional.ofNullable(patch.getTarget()).orElse(route.getTarget()));
                merged.setStripPrefix(Optional.ofNullable(patch.getStripPrefix()).orElse(route.getStripPrefix()));
                merged.setRewrite(Optional.ofNullable(patch.getRewrite()).orElse(route.getRewrite()));
                merged.setGroup(Optional.ofNullable(patch.getGroup()).orElse(route.getGroup()));
                merged.setAuthType(Optional.ofNullable(patch.getAuthType()).orElse(route.getAuthType()));
                merged.setApiKey(Optional.ofNullable(patch.getApiKey()).orElse(route.getApiKey()));
                merged.setRateLimitQps(Optional.ofNullable(patch.getRateLimitQps()).orElse(route.getRateLimitQps()));
                merged.setEnabled(Optional.ofNullable(patch.getEnabled()).orElse(route.getEnabled()));
                merged.setTimeoutMs(Optional.ofNullable(patch.getTimeoutMs()).orElse(route.getTimeoutMs()));
                merged.setCreatedAt(route.getCreatedAt());
                merged.setUpdatedAt(now);
                updatedRule = merged;
                updated.add(merged);
            } else {
                updated.add(route);
            }
        }
        if (updatedRule == null) {
            return Optional.empty();
        }
        RuleSnapshot next = new RuleSnapshot();
        next.setVersion(Instant.now().toString());
        next.setRoutes(updated);
        saveSnapshot(next);
        return Optional.of(updatedRule);
    }

    public boolean deleteRoute(String id) throws IOException {
        RuleSnapshot current = snapshotRef.get();
        List<RouteRule> updated = current.getRoutes().stream()
                .filter(route -> !route.getId().equals(id))
                .toList();
        if (updated.size() == current.getRoutes().size()) {
            return false;
        }
        RuleSnapshot next = new RuleSnapshot();
        next.setVersion(Instant.now().toString());
        next.setRoutes(updated);
        saveSnapshot(next);
        return true;
    }

    public RuleSnapshot replaceSnapshot(RuleSnapshot snapshot) throws IOException {
        String now = Instant.now().toString();
        if (snapshot.getVersion() == null || snapshot.getVersion().isBlank()) {
            snapshot.setVersion(now);
        }
        if (snapshot.getRoutes() != null) {
            for (RouteRule route : snapshot.getRoutes()) {
                if (route.getId() == null || route.getId().isBlank()) {
                    route.setId(UUID.randomUUID().toString());
                }
                if (route.getEnabled() == null) {
                    route.setEnabled(Boolean.TRUE);
                }
                if (route.getCreatedAt() == null || route.getCreatedAt().isBlank()) {
                    route.setCreatedAt(now);
                }
                route.setUpdatedAt(now);
            }
        }
        saveSnapshot(snapshot);
        return snapshot;
    }

    @Scheduled(fixedDelay = 1000)
    public void reloadIfChanged() {
        try {
            long updated = getLastModified();
            if (updated == lastModified) {
                return;
            }
            RuleSnapshot snapshot = storage.readSnapshot();
            snapshotRef.set(snapshot);
            lastModified = updated;
        } catch (IOException ignored) {
            // keep previous snapshot
        }
    }

    private void saveSnapshot(RuleSnapshot snapshot) throws IOException {
        storage.writeSnapshot(snapshot);
        snapshotRef.set(snapshot);
        updateLastModified();
    }

    private void updateLastModified() throws IOException {
        this.lastModified = getLastModified();
    }

    private long getLastModified() throws IOException {
        return Files.getLastModifiedTime(storage.getDataPath()).toMillis();
    }
}
