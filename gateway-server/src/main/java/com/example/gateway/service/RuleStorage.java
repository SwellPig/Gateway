package com.example.gateway.service;

import com.example.gateway.config.GatewayProperties;
import com.example.gateway.model.RuleSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
public class RuleStorage {
    private final ObjectMapper mapper;
    private final Path dataPath;

    public RuleStorage(GatewayProperties properties, ObjectMapper mapper) {
        this.mapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.dataPath = Path.of(properties.getDataPath());
    }

    public RuleSnapshot readSnapshot() throws IOException {
        ensureDataFile();
        return mapper.readValue(dataPath.toFile(), RuleSnapshot.class);
    }

    public void writeSnapshot(RuleSnapshot snapshot) throws IOException {
        ensureDataFile();
        mapper.writeValue(dataPath.toFile(), snapshot);
    }

    public Path getDataPath() {
        return dataPath;
    }

    private void ensureDataFile() throws IOException {
        if (Files.exists(dataPath)) {
            return;
        }
        Files.createDirectories(dataPath.getParent());
        ClassPathResource resource = new ClassPathResource("data/routes.json");
        if (resource.exists()) {
            Files.copy(resource.getInputStream(), dataPath);
        } else {
            RuleSnapshot empty = new RuleSnapshot();
            empty.setVersion(Instant.now().toString());
            mapper.writeValue(dataPath.toFile(), empty);
        }
    }
}
