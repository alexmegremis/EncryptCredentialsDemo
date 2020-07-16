package com.alexmegremis.encryptdemo;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiConfigTest {

    public static final String ACTIVE_PROFILE = "unitTest01";
    @Mock
    private ApplicationContext mockApplicationContext;
    @Mock
    private Environment mockEnvironment;
    @InjectMocks
    private MultiConfig concreteRef;

    @BeforeEach
    void setUp() {
        lenient().when(mockApplicationContext.getEnvironment()).thenReturn(mockEnvironment);
        lenient().when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{ACTIVE_PROFILE});
    }

    @AfterEach
    void tearDown() {
        concreteRef = null;
        mockApplicationContext = null;
    }

    @Test
    void properties() {
    }

    @Test
    void getProfile() {
        String actual = concreteRef.getProfile();
        verify(mockApplicationContext, times(1)).getEnvironment();
        verify(mockEnvironment, times(1)).getActiveProfiles();
        assertEquals(ACTIVE_PROFILE, actual);
    }

    @Test
    void getPropertyFiles() {
        List<Resource> actual = concreteRef.getPropertyFiles();
        assertEquals(2, actual.size());
    }

    @Test
    void getDecrypted() {
    }
}