package com.example.myapp.service;

import com.example.myapp.dto.PatientIntakeRequest;
import com.example.myapp.dto.TriageResultResponse;
import com.example.myapp.exception.PatientNotFoundException;
import com.example.myapp.model.Patient;
import com.example.myapp.model.PriorityLevel;
import com.example.myapp.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriageService {

    private final PatientRepository patientRepository;
    private final TriageScoringEngine scoringEngine;

    @Transactional
    public TriageResultResponse submitPatientCase(PatientIntakeRequest request) {
        log.info("Received intake for patient: {}", request.getName());

        Patient patient = Patient.builder()
                .name(request.getName())
                .age(request.getAge())
                .gender(request.getGender())
                .heartRate(request.getHeartRate())
                .systolicBP(request.getSystolicBP())
                .diastolicBP(request.getDiastolicBP())
                .temperature(request.getTemperature())
                .respiratoryRate(request.getRespiratoryRate())
                .oxygenSaturation(request.getOxygenSaturation())
                .symptoms(request.getSymptoms())
                .chiefComplaint(request.getChiefComplaint())
                .build();

        scoringEngine.score(patient);
        log.info("Patient {} scored {} -> {}", patient.getName(), patient.getTriageScore(), patient.getPriorityLevel());

        Patient saved = patientRepository.save(patient);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TriageResultResponse getTriageResult(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public List<TriageResultResponse> getAllCases() {
        return patientRepository.findAllByOrderByTriageScoreDescSubmittedAtAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TriageResultResponse> getCasesByPriority(PriorityLevel level) {
        return patientRepository.findByPriorityLevelOrderByTriageScoreDesc(level)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TriageResultResponse> getUrgentCases() {
        return patientRepository.findUrgentCases(PriorityLevel.CRITICAL, PriorityLevel.HIGH)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCases", patientRepository.count());
        summary.put("critical", patientRepository.countByPriorityLevel(PriorityLevel.CRITICAL));
        summary.put("high", patientRepository.countByPriorityLevel(PriorityLevel.HIGH));
        summary.put("medium", patientRepository.countByPriorityLevel(PriorityLevel.MEDIUM));
        summary.put("low", patientRepository.countByPriorityLevel(PriorityLevel.LOW));
        return summary;
    }

    private TriageResultResponse toResponse(Patient patient) {
        return TriageResultResponse.builder()
                .id(patient.getId())
                .name(patient.getName())
                .age(patient.getAge())
                .gender(patient.getGender())
                .heartRate(patient.getHeartRate())
                .systolicBP(patient.getSystolicBP())
                .diastolicBP(patient.getDiastolicBP())
                .temperature(patient.getTemperature())
                .respiratoryRate(patient.getRespiratoryRate())
                .oxygenSaturation(patient.getOxygenSaturation())
                .symptoms(patient.getSymptoms())
                .chiefComplaint(patient.getChiefComplaint())
                .triageScore(patient.getTriageScore())
                .priorityLevel(patient.getPriorityLevel())
                .priorityDescription(patient.getPriorityLevel().getDescription())
                .priorityRationale(patient.getPriorityRationale())
                .submittedAt(patient.getSubmittedAt())
                .build();
    }
}
