package com.jklu.campusflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockitoJava26Test {

    @Mock
    private List<String> mockList;

    @Test
    void testMockitoOnJava26() {
        when(mockList.get(0)).thenReturn("CampusFlow");
        assertEquals("CampusFlow", mockList.get(0));
    }
}
