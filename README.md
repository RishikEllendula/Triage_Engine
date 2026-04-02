# Emergency Triage Priority Engine
A full-stack clinical application designed to efficiently triage emergency patients based on real-time vitals and symptoms.

The application calculates a 0-100 priority score and assigns a triage category (CRITICAL, HIGH, MEDIUM, LOW) to help medical staff identify patients requiring immediate intervention.

## Tech Stack
-   **Backend:** Java 17, Spring Boot, Spring Security, Hibernate/JPA
-   **Database:** MySQL (Aiven Cloud)
-   **Frontend:** Vanilla JavaScript, HTML5, CSS3
-   **Infrastructure:** Docker & Docker Compose

## Features
-   **Live Triage Algorithm:** Calculates dynamic priority scores based on vitals (Heart rate, BP, SpO2, Temperature, Resp Rate), age, and specific high-risk symptoms.
-   **Clinical Dashboard:** A real-time dashboard tracking overarching statistics and case distribution.
-   **Sort & Filter Validation:** Instantly filter urgent patients or search through historical cases.
-   **Fully Containerized:** Includes a simple Docker Compose configuration to easily spin up the environment anywhere.

## How to Run locally

### 1. Configure the Environment Details
Ensure your `.env` configuration file exists in the `myapp/` directory with your database connection details. (Note: `.env` is intentionally git-ignored for security).

### 2. Start all services using Docker
You can build and start the entire stack using Docker Compose:
```bash
docker compose up -d --build
```

### 3. Access the Application
-   **Frontend / Clinical Portal:** http://localhost:3000
-   **Backend API Base URL:** http://localhost:8080/api/v1/triage

## Troubleshooting
If you encounter `500 Internal Server Errors` on login, the frontend is successfully triggering its demo-login fallback (`admin@123`). Ensure you do not change the `SecurityConfig.java` to block API requests if the API paths don't officially exist yet!
