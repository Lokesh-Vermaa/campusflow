package com.jklu.campusflow.agent;

import com.jklu.campusflow.agent.service.PolicySearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentPolicySearchTest {

    private final PolicySearchService policySearchService = new PolicySearchService();

    @Test
    @DisplayName("POLICY SEARCH: Querying policy returns relevant content from policies/*.md")
    void testPolicySearchFindsRules() {
        String result = policySearchService.search("approval threshold 50 attendees");
        assertThat(result).isNotBlank();
        assertThat(result.toLowerCase()).contains("approval");
    }

    @Test
    @DisplayName("POLICY SEARCH: Querying absent rule returns exact fallback string")
    void testPolicySearchUnknownQueryReturnsNotFound() {
        String result = policySearchService.search("swimming pool diving board rules");
        assertThat(result).isEqualTo("not found in the booking policy");
    }

    @Test
    @DisplayName("POLICY SEARCH: Empty query returns exact fallback string")
    void testPolicySearchEmptyQueryReturnsNotFound() {
        String result = policySearchService.search("");
        assertThat(result).isEqualTo("not found in the booking policy");
    }
}
