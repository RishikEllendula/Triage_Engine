package com.example.myapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Patient intake data for triage assessment")
public class PatientIntakeRequest {

    @NotBlank(message = "Patient name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(example = "John Doe")
    private String name;

    @Min(value = 0, message = "Age must be 0 or above")
    @Max(value = 130, message = "Age must be 130 or below")
    @Schema(example = "45")
    private int age;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    @Schema(example = "Male")
    private String gender;

    @Min(value = 20, message = "Heart rate must be at least 20")
    @Max(value = 300, message = "Heart rate must be at most 300")
    @Schema(example = "110")
    private int heartRate;

    @Min(value = 50, message = "Systolic BP must be at least 50")
    @Max(value = 250, message = "Systolic BP must be at most 250")
    @Schema(example = "85")
    private int systolicBP;

    @Min(value = 30, message = "Diastolic BP must be at least 30")
    @Max(value = 150, message = "Diastolic BP must be at most 150")
    @Schema(example = "55")
    private int diastolicBP;

    @DecimalMin(value = "30.0", message = "Temperature must be at least 30.0°C")
    @DecimalMax(value = "45.0", message = "Temperature must be at most 45.0°C")
    @Schema(example = "38.9")
    private double temperature;

    @Min(value = 4, message = "Respiratory rate must be at least 4")
    @Max(value = 60, message = "Respiratory rate must be at most 60")
    @Schema(example = "22")
    private int respiratoryRate;

    @Min(value = 60, message = "Oxygen saturation must be at least 60")
    @Max(value = 100, message = "Oxygen saturation must be at most 100")
    @Schema(example = "94")
    private int oxygenSaturation;

    @NotEmpty(message = "At least one symptom must be provided")
    @Schema(example = "[\"chest pain\", \"shortness of breath\"]")
    private List<String> symptoms;

    @NotBlank(message = "Chief complaint is required")
    @Size(max = 500, message = "Chief complaint must be at most 500 characters")
    @Schema(example = "Severe chest pain radiating to the left arm")
    private String chiefComplaint;
}
