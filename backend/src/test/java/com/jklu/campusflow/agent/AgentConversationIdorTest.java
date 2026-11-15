package com.jklu.campusflow.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.agent.dto.RenameConversationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class AgentConversationIdorTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("CONVERSATION CRUD: Create, list, rename, messages, retry, stop, delete lifecycle works")
    void testConversationCrudLifecycle() throws Exception {
        // 1. Create
        MvcResult createResult = mockMvc.perform(post("/api/conversations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RenameConversationRequest("My AI Discussion"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("My AI Discussion")))
                .andReturn();

        String convId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 2. List
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title", is("My AI Discussion")));

        // 3. Get by ID
        mockMvc.perform(get("/api/conversations/" + convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(convId)));

        // 4. Rename
        mockMvc.perform(patch("/api/conversations/" + convId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RenameConversationRequest("Renamed Discussion"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Renamed Discussion")));

        // 5. Get Messages
        mockMvc.perform(get("/api/conversations/" + convId + "/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 6. Retry
        mockMvc.perform(post("/api/conversations/" + convId + "/retry").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(convId)));

        // 7. Stop
        mockMvc.perform(post("/api/conversations/" + convId + "/stop").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("STOPPED")));

        // 8. Delete
        mockMvc.perform(delete("/api/conversations/" + convId).with(csrf()))
                .andExpect(status().isNoContent());

        // 9. Get after deletion -> 404
        mockMvc.perform(get("/api/conversations/" + convId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("CONVERSATION IDOR: Accessing another user's conversation returns identical 404 as non-existent")
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    void testConversationIdorReturns404SameAsNonExistent() throws Exception {
        // Student creates a conversation
        MvcResult createResult = mockMvc.perform(post("/api/conversations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RenameConversationRequest("Confidential Student Chat"))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        String nonExistentId = UUID.randomUUID().toString();

        // 1. Staff queries non-existent conversation -> 404
        MvcResult nonExistentRes = mockMvc.perform(get("/api/conversations/" + nonExistentId)
                        .with(user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andReturn();

        // 2. Staff queries Student's existing conversation -> must return SAME 404 (not 403)
        MvcResult idorRes = mockMvc.perform(get("/api/conversations/" + convId)
                        .with(user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andReturn();

        // Compare error messages
        String nonExistentMsg = objectMapper.readTree(nonExistentRes.getResponse().getContentAsString()).get("message").asText();
        String idorMsg = objectMapper.readTree(idorRes.getResponse().getContentAsString()).get("message").asText();

        assertThat(nonExistentMsg).isEqualTo("Conversation not found: " + nonExistentId);
        assertThat(idorMsg).isEqualTo("Conversation not found: " + convId);

        // 3. Same check for messages endpoint:
        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                        .with(user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));

        // 4. Same check for rename endpoint:
        mockMvc.perform(patch("/api/conversations/" + convId)
                        .with(csrf())
                        .with(user("staff@demo.jklu.test").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RenameConversationRequest("Hacked Title"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));

        // 5. Same check for delete endpoint:
        mockMvc.perform(delete("/api/conversations/" + convId)
                        .with(csrf())
                        .with(user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }
}
