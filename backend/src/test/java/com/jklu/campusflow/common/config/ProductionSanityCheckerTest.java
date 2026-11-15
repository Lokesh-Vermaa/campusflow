package com.jklu.campusflow.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSanityCheckerTest {

    @Test
    @DisplayName("Prod startup passes with valid password, strict origins, and no demo profile")
    void testProdStartupSuccess() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionSanityChecker checker = new ProductionSanityChecker(env, "StrongProdPass#2026", "https://campusflow.jklu.edu.in");

        assertThatCode(checker::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Prod startup fails if DB_PASSWORD is blank")
    void testProdStartupFailsOnBlankPassword() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionSanityChecker checker = new ProductionSanityChecker(env, "", "https://campusflow.jklu.edu.in");

        assertThatThrownBy(checker::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    @DisplayName("Prod startup fails if ALLOWED_ORIGINS has wildcard")
    void testProdStartupFailsOnWildcardOrigin() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionSanityChecker checker = new ProductionSanityChecker(env, "StrongProdPass#2026", "*");

        assertThatThrownBy(checker::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALLOWED_ORIGINS");
    }

    @Test
    @DisplayName("Prod startup fails if demo profile is active")
    void testProdStartupFailsOnDemoProfileActive() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod", "demo");
        ProductionSanityChecker checker = new ProductionSanityChecker(env, "StrongProdPass#2026", "https://campusflow.jklu.edu.in");

        assertThatThrownBy(checker::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'demo' profile");
    }

    @Test
    @DisplayName("Non-prod environments bypass prod checks")
    void testDevProfileBypassesChecks() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        ProductionSanityChecker checker = new ProductionSanityChecker(env, "", "*");

        assertThatCode(checker::validate).doesNotThrowAnyException();
    }
}
