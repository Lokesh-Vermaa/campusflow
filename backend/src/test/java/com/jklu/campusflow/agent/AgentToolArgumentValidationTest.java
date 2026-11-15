package com.jklu.campusflow.agent;

import com.jklu.campusflow.agent.tools.AgentTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AgentToolArgumentValidationTest {

    private final AgentTools agentTools = new AgentTools(
            Mockito.mock(com.jklu.campusflow.availability.service.RoomAvailabilityService.class),
            Mockito.mock(com.jklu.campusflow.campus.service.CampusDataService.class),
            new com.jklu.campusflow.agent.service.PolicySearchService()
    );

    @Test
    @DisplayName("AGENT TOOLS: findFreeRooms rejects null parameters with IllegalArgumentException")
    void testFindFreeRoomsRejectsNullArguments() {
        assertThatThrownBy(() -> agentTools.findFreeRooms(null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AGENT TOOLS: checkRoomAvailability rejects null parameters with IllegalArgumentException")
    void testCheckRoomAvailabilityRejectsNullArguments() {
        assertThatThrownBy(() -> agentTools.checkRoomAvailability(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
