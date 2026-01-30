package com.example.gateway.model;

import java.util.ArrayList;
import java.util.List;

public class RuleSnapshot {
    private String version;
    private List<RouteRule> routes = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<RouteRule> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteRule> routes) {
        this.routes = routes;
    }
}
