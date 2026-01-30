package com.example.gateway.web;

import com.example.gateway.model.RouteRule;
import com.example.gateway.model.RuleSnapshot;
import com.example.gateway.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/routes")
public class AdminController {
    private final RuleService ruleService;

    public AdminController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public RuleSnapshot list() {
        return ruleService.getSnapshot();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid RouteRule route) throws IOException {
        if (route.getPath() == null || route.getTarget() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "path 与 target 为必填字段"));
        }
        RouteRule created = ruleService.addRoute(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody RouteRule route) throws IOException {
        Optional<RouteRule> updated = ruleService.updateRoute(id, route);
        return updated.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "未找到该规则")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) throws IOException {
        boolean deleted = ruleService.deleteRoute(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "未找到该规则"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
