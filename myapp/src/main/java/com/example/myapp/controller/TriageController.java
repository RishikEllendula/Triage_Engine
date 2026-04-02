package com.example.myapp.controller;

import com.example.myapp.dto.ApiErrorResponse;
import com.example.myapp.dto.PatientIntakeRequest;
import com.example.myapp.dto.TriageResultResponse;
import com.example.myapp.model.PriorityLevel;
import com.example.myapp.service.TriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/triage")
@RequiredArgsConstructor
@Tag(name = "Triage", description = "Emergency patient triage submission and result retrieval APIs")
public class TriageController {

    private final TriageService triageService;

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new patient case for triage", description = "Accepts patient demographics, vital signs, and symptoms. "
            +
            "Computes a triage score and classifies the case into CRITICAL, HIGH, MEDIUM, or LOW.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Patient case submitted and triaged successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TriageResultResponse> submitCase(
            @Valid @RequestBody PatientIntakeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(triageService.submitPatientCase(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve triage result by patient ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Triage result found"),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TriageResultResponse> getResult(
            @Parameter(description = "Patient record ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(triageService.getTriageResult(id));
    }

    @GetMapping("/cases")
    @Operation(summary = "List all triage cases sorted by urgency")
    public ResponseEntity<List<TriageResultResponse>> getAllCases() {
        return ResponseEntity.ok(triageService.getAllCases());
    }

    @GetMapping("/cases/priority/{level}")
    @Operation(summary = "Filter cases by priority level")
    public ResponseEntity<List<TriageResultResponse>> getCasesByPriority(
            @Parameter(description = "Priority level", example = "CRITICAL") @PathVariable String level) {
        try {
            PriorityLevel priorityLevel = PriorityLevel.valueOf(level.toUpperCase());
            return ResponseEntity.ok(triageService.getCasesByPriority(priorityLevel));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority level: " + level +
                    ". Valid values are: CRITICAL, HIGH, MEDIUM, LOW");
        }
    }

    @GetMapping("/cases/urgent")
    @Operation(summary = "Get all urgent cases (CRITICAL + HIGH)")
    public ResponseEntity<List<TriageResultResponse>> getUrgentCases() {
        return ResponseEntity.ok(triageService.getUrgentCases());
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get triage dashboard summary")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(triageService.getDashboardSummary());
    }
}
