package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private String gender;

    private int heartRate;
    private int systolicBP;
    private int diastolicBP;
    private double temperature;
    private int respiratoryRate;
    private int oxygenSaturation;

    @Convert(converter = com.example.myapp.util.StringListConverter.class)
    @Column(name = "symptoms", length = 1000)
    private List<String> symptoms;

    private String chiefComplaint;
    private int triageScore;

    @Enumerated(EnumType.STRING)
    private PriorityLevel priorityLevel;

    @Column(length = 2000)
    private String priorityRationale;

    @Column(updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
