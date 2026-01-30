package com.example.gateway.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticController {
    @GetMapping("/")
    public ResponseEntity<Resource> index() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/index.html"));
    }

    @GetMapping("/app.js")
    public ResponseEntity<Resource> app() {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(new ClassPathResource("static/app.js"));
    }

    @GetMapping("/styles.css")
    public ResponseEntity<Resource> styles() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_CSS)
                .body(new ClassPathResource("static/styles.css"));
    }
}
