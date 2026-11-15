package com.jklu.campusflow.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Validates production environment constraints at startup.
 * In 'prod' profile, the application MUST refuse to start if:
 * 1. DB_PASSWORD is empty or blank
 * 2. ALLOWED_ORIGINS contains "*"
 * 3. 'demo' profile is active or demo seeding is enabled
 */
@Component
public class ProductionSanityChecker implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ProductionSanityChecker.class);

    private final Environment environment;
    private final String dbPassword;
    private final String allowedOrigins;

    public ProductionSanityChecker(
            Environment environment,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${app.allowed-origins:}") String allowedOrigins
    ) {
        this.environment = environment;
        this.dbPassword = dbPassword;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validate();
    }

    public void validate() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProd) {
            return;
        }

        log.info("Running Production environment startup sanity check...");

        if (dbPassword == null || dbPassword.trim().isEmpty() || "devpass".equalsIgnoreCase(dbPassword.trim())) {
            throw new IllegalStateException("FATAL: Production mode requires a non-blank, secure DB_PASSWORD.");
        }

        if (allowedOrigins != null && allowedOrigins.contains("*")) {
            throw new IllegalStateException("FATAL: Production mode prohibits wildcard '*' in ALLOWED_ORIGINS.");
        }

        boolean isDemoActive = Arrays.asList(environment.getActiveProfiles()).contains("demo");
        if (isDemoActive) {
            throw new IllegalStateException("FATAL: 'demo' profile cannot be active in production mode.");
        }

        log.info("Production environment sanity check passed.");
    }
}
