package com.example.myapp.dto;

import com.example.myapp.model.PriorityLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Complete triage assessment result for a patient")
public class TriageResultResponse {

    private Long id;
    private String name;
    private int age;
    private String gender;
    private int heartRate;
    private int systolicBP;
    private int diastolicBP;
    private double temperature;
    private int respiratoryRate;
    private int oxygenSaturation;
    private List<String> symptoms;
    private String chiefComplaint;
    private int triageScore;
    private PriorityLevel priorityLevel;
    private String priorityDescription;
    private String priorityRationale;
    private LocalDateTime submittedAt;
}
