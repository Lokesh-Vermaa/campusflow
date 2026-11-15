package com.jklu.campusflow.content.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    @GetMapping("/announcement")
    public ResponseEntity<Map<String, Object>> getAnnouncement() {
        return ResponseEntity.ok(Map.of(
                "id", "ann-1",
                "message", "Admissions peak week: book venues early",
                "linkText", "See booking rules",
                "linkHref", "/rules",
                "active", true
        ));
    }
}
