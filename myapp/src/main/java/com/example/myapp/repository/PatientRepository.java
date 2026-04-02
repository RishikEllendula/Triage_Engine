package com.example.myapp.repository;

import com.example.myapp.model.Patient;
import com.example.myapp.model.PriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByPriorityLevelOrderByTriageScoreDesc(PriorityLevel priorityLevel);

    List<Patient> findAllByOrderByTriageScoreDescSubmittedAtAsc();

    @Query("SELECT p FROM Patient p WHERE p.priorityLevel IN (:critical, :high) ORDER BY p.triageScore DESC")
    List<Patient> findUrgentCases(@Param("critical") PriorityLevel critical,
            @Param("high") PriorityLevel high);

    long countByPriorityLevel(PriorityLevel priorityLevel);
}
