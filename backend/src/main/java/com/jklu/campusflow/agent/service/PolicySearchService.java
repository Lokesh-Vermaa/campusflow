package com.jklu.campusflow.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class PolicySearchService {

    private static final Logger log = LoggerFactory.getLogger(PolicySearchService.class);
    private static final Set<String> STOP_WORDS = Set.of(
            "rules", "rule", "policy", "policies", "and", "the", "for", "with", "how", "what", "can", "are", "does", "query"
    );

    private final Map<String, String> policyFiles = new LinkedHashMap<>();

    public PolicySearchService() {
        loadPolicies();
    }

    private void loadPolicies() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:policies/*.md");
            for (Resource r : resources) {
                try (InputStream is = r.getInputStream()) {
                    String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    policyFiles.put(r.getFilename(), text);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load policy files: {}", e.getMessage());
        }
    }

    public String search(String query) {
        if (query == null || query.trim().length() < 3) {
            return "not found in the booking policy";
        }

        String lowerQuery = query.toLowerCase();
        String[] rawKeywords = lowerQuery.split("\\s+");
        List<String> meaningfulKeywords = Arrays.stream(rawKeywords)
                .filter(k -> k.length() >= 2 && !STOP_WORDS.contains(k))
                .toList();

        if (meaningfulKeywords.isEmpty()) {
            return "not found in the booking policy";
        }

        List<String> matches = new ArrayList<>();

        for (Map.Entry<String, String> entry : policyFiles.entrySet()) {
            String content = entry.getValue();
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String lowerLine = trimmed.toLowerCase();

                long matchCount = meaningfulKeywords.stream()
                        .filter(kw -> lowerLine.matches(".*\\b" + Pattern.quote(kw) + "\\b.*"))
                        .count();

                if (meaningfulKeywords.size() >= 2 && matchCount >= 2) {
                    matches.add(trimmed);
                } else if (meaningfulKeywords.size() == 1 && matchCount == 1) {
                    matches.add(trimmed);
                }
            }
        }

        if (matches.isEmpty()) {
            return "not found in the booking policy";
        }

        return String.join("\n", matches.subList(0, Math.min(5, matches.size())));
    }
}
