package com.example.gateway.model;

import java.util.List;

public class RouteRule {
    private String id;
    private String path;
    private List<String> methods;
    private String target;
    private Integer stripPrefix;
    private String rewrite;
    private String group;
    private String authType;
    private String apiKey;
    private Integer rateLimitQps;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(Integer stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public String getRewrite() {
        return rewrite;
    }

    public void setRewrite(String rewrite) {
        this.rewrite = rewrite;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getRateLimitQps() {
        return rateLimitQps;
    }

    public void setRateLimitQps(Integer rateLimitQps) {
        this.rateLimitQps = rateLimitQps;
    }
}
