package com.example.myapp;

import com.example.myapp.model.Patient;
import com.example.myapp.model.PriorityLevel;
import com.example.myapp.service.TriageScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriageScoringEngineTest {

        private TriageScoringEngine engine;

        @BeforeEach
        void setUp() {
                engine = new TriageScoringEngine();
        }

        private Patient buildPatient(int age, int hr, int sysBP, int spo2, int rr,
                        double temp, List<String> symptoms, String complaint) {
                return Patient.builder()
                                .name("Test Patient")
                                .age(age)
                                .gender("Male")
                                .heartRate(hr)
                                .systolicBP(sysBP)
                                .diastolicBP(80)
                                .oxygenSaturation(spo2)
                                .respiratoryRate(rr)
                                .temperature(temp)
                                .symptoms(symptoms)
                                .chiefComplaint(complaint)
                                .build();
        }

        @Test
        @DisplayName("Cardiac arrest patient should be CRITICAL")
        void testCriticalCardiacCase() {
                Patient p = buildPatient(65, 155, 65, 82, 32, 40.5,
                                Arrays.asList("cardiac arrest", "loss of consciousness", "diaphoresis"),
                                "Patient collapsed with suspected cardiac arrest");
                engine.score(p);
                assertEquals(PriorityLevel.CRITICAL, p.getPriorityLevel());
                assertTrue(p.getTriageScore() >= 75);
                assertNotNull(p.getPriorityRationale());
        }

        @Test
        @DisplayName("Stable patient with mild symptoms should be LOW or MEDIUM")
        void testLowPriorityCase() {
                Patient p = buildPatient(30, 88, 120, 98, 16, 37.8,
                                Arrays.asList("mild headache", "runny nose"),
                                "Minor headache and cold symptoms for 2 days");
                engine.score(p);
                assertTrue(p.getPriorityLevel() == PriorityLevel.LOW
                                || p.getPriorityLevel() == PriorityLevel.MEDIUM);
                assertTrue(p.getTriageScore() < 50);
        }

        @Test
        @DisplayName("Elderly patient with chest pain should be at least HIGH")
        void testElderlyChestPain() {
                Patient p = buildPatient(75, 105, 95, 92, 22, 37.2,
                                Arrays.asList("chest pain", "shortness of breath"),
                                "Chest pain radiating to the arm for 20 minutes");
                engine.score(p);
                assertTrue(p.getPriorityLevel() == PriorityLevel.CRITICAL
                                || p.getPriorityLevel() == PriorityLevel.HIGH);
                assertTrue(p.getTriageScore() >= 50);
        }

        @Test
        @DisplayName("Score should never exceed 100")
        void testScoreNeverExceeds100() {
                Patient p = buildPatient(1, 200, 50, 60, 50, 44.0,
                                Arrays.asList("cardiac arrest", "seizure", "anaphylaxis", "stroke",
                                                "chest pain", "loss of consciousness", "paralysis"),
                                "Cardiac arrest, unresponsive, multiple traumas");
                engine.score(p);
                assertTrue(p.getTriageScore() <= 100, "Score should be capped at 100");
        }

        @Test
        @DisplayName("Rationale should not be empty after scoring")
        void testRationaleNotEmpty() {
                Patient p = buildPatient(40, 90, 120, 98, 16, 37.0,
                                Arrays.asList("vomiting", "dizziness"),
                                "Feeling unwell, vomiting since this morning");
                engine.score(p);
                assertNotNull(p.getPriorityRationale());
                assertFalse(p.getPriorityRationale().isBlank());
        }
}
