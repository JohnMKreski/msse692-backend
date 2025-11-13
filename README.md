# Ark Valley Events – Backend

[//]: # (![CI]&#40;https://github.com/JohnMKreski/msse692-backend/actions/workflows/ci.yml/badge.svg&#41;)

A backend service for the **Ark Valley Events** platform. This project provides a REST API for managing users and events, built with **Spring Boot**, **PostgreSQL**, and secured with **Firebase Authentication**.

---

## Tech Stack

* **Java 21**
* **Spring Boot 3.5.6**

    * Spring Web (REST API)
    * Spring Data JPA (ORM)
    * Spring Security + OAuth2 Resource Server (Firebase JWT validation)
    * Validation (Jakarta Bean Validation)
    * Actuator (health & metrics)
* **Database**

    * PostgreSQL (production)
    * H2 (in-memory for development/testing)
* **Build**

    * Maven
    * Spotless (Google Java Format for code style)
* **CI/CD**

    * GitHub Actions (Spotless check + Maven build/test)

---

## Project Structure

```
src/main/java/com/arkvalleyevents/msse692_backend
 ├── controller/    # REST controllers
 ├── service/       # Business logic
 ├── repository/    # Spring Data repositories
 ├── model/         # JPA entities
 ├── dto/           # Data Transfer Objects
 └── Msse692BackendApplication.java
```

---

## Getting Started

### Prerequisites

* JDK 21+
* Maven 3.9+
* PostgreSQL (local or cloud instance)

### Run Locally (Dev Profile with H2)

```bash
mvn -Pdev spring-boot:run
```

API will be available at: [http://localhost:8080](http://localhost:8080)

### Run with PostgreSQL (Prod Profile)

Set environment variables before starting:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/arkve
export DB_USER=youruser
export DB_PASS=yourpass

mvn -Pprod spring-boot:run

### Run with PostgreSQL (Local Profile)

For local development using your local Postgres and Firebase tokens, set the same env vars, then:

```bash
mvn -Plocal spring-boot:run
```

### Windows convenience scripts (clean + run)

From the `msse692-backend` folder, you can also use the provided `.cmd` scripts which perform a clean compile before starting:

```cmd
./run-dev.cmd
./run-local.cmd
./run-prod.cmd
REM Or generic:
./run-profile.cmd dev|local|prod
```
```

---

Demo Mode

This project is set up as a developer demo. When you start the application:

The backend will automatically open Swagger UI in your default browser.

Swagger UI is available at: http://localhost:8080/swagger-ui.html

The dev profile uses an in-memory H2 database (no external setup needed).

CORS is wide open in dev mode so you can test with Angular or Postman.


### Profiles

* dev (default)
  * H2 database
  * CORS open (*)
  * Auto Swagger browser launch

* prod
  * PostgreSQL database
  * CORS restricted to allowed origins
  * No auto-browser launch (server-friendly)

---

## Firebase Authentication

This backend validates Firebase-issued JWTs. Example `application.yml` config:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://securetoken.google.com/<your-project-id>
          jwk-set-uri: https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com
```

---

## Testing

* Unit & integration tests:

  ```bash
  mvn test
  ```
* Formatting check (Spotless):

  ```bash
  mvn spotless:check
  ```
* Auto-fix formatting:

  ```bash
  mvn spotless:apply
  ```

---

## Deployment

* CI/CD via GitHub Actions (`.github/workflows/ci.yml`)
* Target deployment: AWS (Amplify for frontend, Elastic Beanstalk / ECS / RDS for backend)

---

## License

This project is developed as part of the **MSSE 692 Practicum I** at Regis University.
