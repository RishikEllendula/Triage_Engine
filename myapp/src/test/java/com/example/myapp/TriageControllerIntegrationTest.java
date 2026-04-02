package com.example.myapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.myapp.controller.TriageController;
import com.example.myapp.dto.PatientIntakeRequest;
import com.example.myapp.dto.TriageResultResponse;
import com.example.myapp.model.PriorityLevel;
import com.example.myapp.service.TriageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TriageController.class)
class TriageControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        // ObjectMapper is not a bean in the @WebMvcTest slice in Spring Boot 4 —
        // instantiate directly
        private final ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @MockitoBean
        private TriageService triageService;

        private PatientIntakeRequest buildCriticalPatient() {
                return PatientIntakeRequest.builder()
                                .name("Jane Smith")
                                .age(68)
                                .gender("Female")
                                .heartRate(148)
                                .systolicBP(78)
                                .diastolicBP(50)
                                .temperature(40.2)
                                .respiratoryRate(28)
                                .oxygenSaturation(87)
                                .symptoms(Arrays.asList("chest pain", "shortness of breath", "diaphoresis"))
                                .chiefComplaint("Severe chest pain and difficulty breathing for the past 20 minutes")
                                .build();
        }

        private TriageResultResponse buildMockResponse() {
                return TriageResultResponse.builder()
                                .id(1L)
                                .name("Jane Smith")
                                .age(68)
                                .gender("Female")
                                .heartRate(148)
                                .systolicBP(78)
                                .diastolicBP(50)
                                .temperature(40.2)
                                .respiratoryRate(28)
                                .oxygenSaturation(87)
                                .symptoms(Arrays.asList("chest pain", "shortness of breath", "diaphoresis"))
                                .chiefComplaint("Severe chest pain and difficulty breathing for the past 20 minutes")
                                .triageScore(92)
                                .priorityLevel(PriorityLevel.CRITICAL)
                                .priorityDescription(PriorityLevel.CRITICAL.getDescription())
                                .priorityRationale("Critically abnormal heart rate; Critical hypoxia")
                                .submittedAt(LocalDateTime.now())
                                .build();
        }

        @Test
        @DisplayName("POST /submit returns 201 and triage result")
        void testSubmitCase() throws Exception {
                when(triageService.submitPatientCase(any(PatientIntakeRequest.class)))
                                .thenReturn(buildMockResponse());

                mockMvc.perform(post("/api/v1/triage/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildCriticalPatient())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.priorityLevel").exists())
                                .andExpect(jsonPath("$.triageScore").isNumber());
        }

        @Test
        @DisplayName("GET /dashboard returns summary counts")
        void testDashboard() throws Exception {
                when(triageService.getDashboardSummary())
                                .thenReturn(Map.of("totalCases", 5L, "critical", 2L, "high", 1L, "medium", 1L, "low",
                                                1L));

                mockMvc.perform(get("/api/v1/triage/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalCases").isNumber());
        }

        @Test
        @DisplayName("GET /{id} for non-existent patient returns 404")
        void testGetNonExistentPatient() throws Exception {
                when(triageService.getTriageResult(99999L))
                                .thenThrow(
                                                new com.example.myapp.exception.PatientNotFoundException(
                                                                "Patient not found with ID: 99999"));

                mockMvc.perform(get("/api/v1/triage/99999"))
                                .andExpect(status().isNotFound());
        }
}
