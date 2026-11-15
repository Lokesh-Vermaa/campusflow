package com.jklu.campusflow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoSecretsCheckTest {

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i)(password|secret|apikey|token)\\s*=\\s*['\"][^'\"]{6,}['\"]"),
            Pattern.compile("-----BEGIN (RSA |EC )?PRIVATE KEY-----")
    );

    @Test
    void verifyNoHardcodedSecretsInSourceOrResources() throws IOException {
        Path srcDir = Paths.get("src");
        if (!Files.exists(srcDir)) {
            srcDir = Paths.get("backend", "src");
        }

        try (Stream<Path> paths = Files.walk(srcDir)) {
            List<Path> matchingFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().endsWith("NoSecretsCheckTest.java"))
                    .toList();

            for (Path file : matchingFiles) {
                String content = Files.readString(file);
                for (Pattern pattern : SECRET_PATTERNS) {
                    boolean found = pattern.matcher(content).find();
                    assertTrue(!found, "Hardcoded secret pattern matched in file: " + file);
                }
            }
        }
    }
}
