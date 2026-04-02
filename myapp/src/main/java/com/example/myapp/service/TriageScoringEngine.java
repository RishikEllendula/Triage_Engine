package com.example.myapp.service;

import com.example.myapp.model.Patient;
import com.example.myapp.model.PriorityLevel;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rule-based triage scoring engine.
 *
 * <p>
 * Scoring categories (total max = 100 points):
 * <ul>
 * <li>Vital Signs Abnormality – up to 40 pts</li>
 * <li>High-Risk Symptom Flags – up to 30 pts</li>
 * <li>Age Vulnerability Factor – up to 15 pts</li>
 * <li>Chief Complaint Keywords – up to 15 pts</li>
 * </ul>
 *
 * <p>
 * Priority thresholds:
 * <ul>
 * <li>CRITICAL → score >= 75</li>
 * <li>HIGH → score >= 50</li>
 * <li>MEDIUM → score >= 25</li>
 * <li>LOW → score &lt; 25</li>
 * </ul>
 */
@Component
public class TriageScoringEngine {

    // High-risk symptom keywords mapped to their point values
    private static final Map<String, Integer> SYMPTOM_SCORES;

    // Critical complaint keywords (chief complaint)
    private static final Set<String> CRITICAL_COMPLAINT_KEYWORDS;
    private static final Set<String> HIGH_COMPLAINT_KEYWORDS;

    static {
        SYMPTOM_SCORES = new LinkedHashMap<>();

        // Immediately life-threatening (15 pts each)
        SYMPTOM_SCORES.put("cardiac arrest", 15);
        SYMPTOM_SCORES.put("respiratory arrest", 15);
        SYMPTOM_SCORES.put("unconscious", 15);
        SYMPTOM_SCORES.put("unresponsive", 15);
        SYMPTOM_SCORES.put("anaphylaxis", 15);
        SYMPTOM_SCORES.put("seizure", 12);
        SYMPTOM_SCORES.put("stroke", 12);
        SYMPTOM_SCORES.put("severe hemorrhage", 12);
        SYMPTOM_SCORES.put("major trauma", 12);

        // Urgent (8 pts each)
        SYMPTOM_SCORES.put("chest pain", 8);
        SYMPTOM_SCORES.put("shortness of breath", 8);
        SYMPTOM_SCORES.put("difficulty breathing", 8);
        SYMPTOM_SCORES.put("severe abdominal pain", 8);
        SYMPTOM_SCORES.put("altered mental status", 8);
        SYMPTOM_SCORES.put("severe headache", 7);
        SYMPTOM_SCORES.put("diaphoresis", 6);
        SYMPTOM_SCORES.put("syncope", 7);
        SYMPTOM_SCORES.put("fainting", 6);
        SYMPTOM_SCORES.put("coughing blood", 7);
        SYMPTOM_SCORES.put("vomiting blood", 7);
        SYMPTOM_SCORES.put("loss of consciousness", 10);
        SYMPTOM_SCORES.put("paralysis", 8);

        // Semi-urgent (4 pts each)
        SYMPTOM_SCORES.put("fever", 4);
        SYMPTOM_SCORES.put("vomiting", 3);
        SYMPTOM_SCORES.put("diarrhea", 2);
        SYMPTOM_SCORES.put("dizziness", 4);
        SYMPTOM_SCORES.put("confusion", 5);
        SYMPTOM_SCORES.put("back pain", 3);
        SYMPTOM_SCORES.put("abdominal pain", 4);
        SYMPTOM_SCORES.put("palpitations", 4);
        SYMPTOM_SCORES.put("numbness", 4);
        SYMPTOM_SCORES.put("swelling", 2);
        SYMPTOM_SCORES.put("weakness", 3);

        // Non-urgent (1 pt each)
        SYMPTOM_SCORES.put("mild headache", 1);
        SYMPTOM_SCORES.put("sore throat", 1);
        SYMPTOM_SCORES.put("cough", 1);
        SYMPTOM_SCORES.put("runny nose", 1);
        SYMPTOM_SCORES.put("rash", 2);
        SYMPTOM_SCORES.put("minor cut", 1);
        SYMPTOM_SCORES.put("bruise", 1);

        CRITICAL_COMPLAINT_KEYWORDS = new HashSet<>(Arrays.asList(
                "heart attack", "stroke", "can't breathe", "cannot breathe",
                "not breathing", "unconscious", "unresponsive", "severe bleeding",
                "anaphylaxis", "overdose", "poisoning", "seizure", "cardiac"));

        HIGH_COMPLAINT_KEYWORDS = new HashSet<>(Arrays.asList(
                "chest pain", "chest tightness", "difficulty breathing",
                "shortness of breath", "severe pain", "head injury",
                "broken bone", "fracture", "allergic reaction", "high fever"));
    }

    /**
     * Scores a patient and sets their triageScore, priorityLevel, and
     * priorityRationale.
     */
    public void score(Patient patient) {
        List<String> rationale = new ArrayList<>();
        int totalScore = 0;

        // 1. Vital signs scoring (max 40 pts)
        int vitalScore = scoreVitals(patient, rationale);
        totalScore += Math.min(vitalScore, 40);

        // 2. Symptom scoring (max 30 pts)
        int symptomScore = scoreSymptoms(patient, rationale);
        totalScore += Math.min(symptomScore, 30);

        // 3. Age vulnerability (max 15 pts)
        int ageScore = scoreAge(patient.getAge(), rationale);
        totalScore += Math.min(ageScore, 15);

        // 4. Chief complaint keywords (max 15 pts)
        int complaintScore = scoreChiefComplaint(patient.getChiefComplaint(), rationale);
        totalScore += Math.min(complaintScore, 15);

        // Clamp final score to 100
        totalScore = Math.min(totalScore, 100);

        patient.setTriageScore(totalScore);
        patient.setPriorityLevel(determinePriority(totalScore));
        patient.setPriorityRationale(String.join("; ", rationale));
    }

    private int scoreVitals(Patient p, List<String> rationale) {
        int score = 0;

        // Heart rate
        if (p.getHeartRate() >= 150 || p.getHeartRate() < 40) {
            score += 15;
            rationale.add("Critically abnormal heart rate (" + p.getHeartRate() + " bpm)");
        } else if (p.getHeartRate() >= 120 || p.getHeartRate() < 50) {
            score += 10;
            rationale.add("Significantly abnormal heart rate (" + p.getHeartRate() + " bpm)");
        } else if (p.getHeartRate() > 100 || p.getHeartRate() < 60) {
            score += 5;
            rationale.add("Mildly abnormal heart rate (" + p.getHeartRate() + " bpm)");
        }

        // Systolic BP
        if (p.getSystolicBP() < 70 || p.getSystolicBP() > 200) {
            score += 15;
            rationale.add("Critically abnormal systolic BP (" + p.getSystolicBP() + " mmHg)");
        } else if (p.getSystolicBP() < 90 || p.getSystolicBP() > 180) {
            score += 10;
            rationale.add("Significantly abnormal systolic BP (" + p.getSystolicBP() + " mmHg)");
        } else if (p.getSystolicBP() < 100 || p.getSystolicBP() > 160) {
            score += 5;
            rationale.add("Mildly abnormal systolic BP (" + p.getSystolicBP() + " mmHg)");
        }

        // Oxygen Saturation
        if (p.getOxygenSaturation() < 85) {
            score += 15;
            rationale.add("Critical hypoxia (SpO2 " + p.getOxygenSaturation() + "%)");
        } else if (p.getOxygenSaturation() < 90) {
            score += 10;
            rationale.add("Severe hypoxia (SpO2 " + p.getOxygenSaturation() + "%)");
        } else if (p.getOxygenSaturation() < 94) {
            score += 5;
            rationale.add("Mild hypoxia (SpO2 " + p.getOxygenSaturation() + "%)");
        }

        // Respiratory Rate
        if (p.getRespiratoryRate() > 30 || p.getRespiratoryRate() < 8) {
            score += 10;
            rationale.add("Critically abnormal respiratory rate (" + p.getRespiratoryRate() + " breaths/min)");
        } else if (p.getRespiratoryRate() > 24 || p.getRespiratoryRate() < 10) {
            score += 6;
            rationale.add("Elevated respiratory rate (" + p.getRespiratoryRate() + " breaths/min)");
        } else if (p.getRespiratoryRate() > 20) {
            score += 3;
            rationale.add("Slightly elevated respiratory rate (" + p.getRespiratoryRate() + " breaths/min)");
        }

        // Temperature
        if (p.getTemperature() >= 40.0 || p.getTemperature() < 35.0) {
            score += 10;
            rationale.add("Critically abnormal temperature (" + p.getTemperature() + "°C)");
        } else if (p.getTemperature() >= 39.0 || p.getTemperature() < 35.5) {
            score += 6;
            rationale.add("Significantly abnormal temperature (" + p.getTemperature() + "°C)");
        } else if (p.getTemperature() > 37.5) {
            score += 2;
            rationale.add("Mildly elevated temperature (" + p.getTemperature() + "°C)");
        }

        return score;
    }

    private int scoreSymptoms(Patient patient, List<String> rationale) {
        if (patient.getSymptoms() == null || patient.getSymptoms().isEmpty()) {
            return 0;
        }

        int score = 0;
        List<String> matchedSymptoms = new ArrayList<>();

        for (String symptom : patient.getSymptoms()) {
            String normalized = symptom.toLowerCase().trim();
            for (Map.Entry<String, Integer> entry : SYMPTOM_SCORES.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    score += entry.getValue();
                    matchedSymptoms.add(symptom + " (+" + entry.getValue() + " pts)");
                    break; // prevent double-counting for a single symptom
                }
            }
        }

        if (!matchedSymptoms.isEmpty()) {
            rationale.add("High-risk symptoms identified: " + String.join(", ", matchedSymptoms));
        }

        return score;
    }

    private int scoreAge(int age, List<String> rationale) {
        if (age < 1) {
            rationale.add("Neonate/infant age flag (+15 pts)");
            return 15;
        } else if (age <= 5) {
            rationale.add("Young child age flag (+12 pts)");
            return 12;
        } else if (age >= 80) {
            rationale.add("Advanced age vulnerability (>=80 years, +12 pts)");
            return 12;
        } else if (age >= 65) {
            rationale.add("Elderly patient flag (>=65 years, +8 pts)");
            return 8;
        } else if (age <= 12) {
            rationale.add("Pediatric flag (+5 pts)");
            return 5;
        }
        return 0;
    }

    private int scoreChiefComplaint(String complaint, List<String> rationale) {
        if (complaint == null || complaint.isBlank()) {
            return 0;
        }
        String lower = complaint.toLowerCase();

        for (String keyword : CRITICAL_COMPLAINT_KEYWORDS) {
            if (lower.contains(keyword)) {
                rationale.add("Critical complaint keyword matched: \"" + keyword + "\" (+15 pts)");
                return 15;
            }
        }
        for (String keyword : HIGH_COMPLAINT_KEYWORDS) {
            if (lower.contains(keyword)) {
                rationale.add("High-urgency complaint keyword matched: \"" + keyword + "\" (+10 pts)");
                return 10;
            }
        }
        return 2; // baseline for having a complaint
    }

    private PriorityLevel determinePriority(int score) {
        if (score >= 75)
            return PriorityLevel.CRITICAL;
        if (score >= 50)
            return PriorityLevel.HIGH;
        if (score >= 25)
            return PriorityLevel.MEDIUM;
        return PriorityLevel.LOW;
    }
}
