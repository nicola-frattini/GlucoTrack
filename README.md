# GlucoTrack

A JavaFX desktop application for diabetes management, built to support **patients**, **doctors**, and **administrators** through role-based dashboards and local clinical data persistence.

Project made for the "Software Engineer" module at "Università degli studi di Verona"

## Table of Contents

- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Database](#database)
- [UI and Navigation](#ui-and-navigation)
- [Architecture Notes](#architecture-notes)
- [Documentation](#documentation)
- [Current Development Focus](#current-development-focus)
- [Contributing](#contributing)
- [License](#license)

## Project Overview

GlucoTrack is a multi-role desktop system focused on day-to-day diabetes care workflows:

- Patients can track glucose readings, symptoms, and therapies.
- Doctors can review patient-related data and manage treatments.
- Administrators can perform user-management operations.

The project is implemented in Java with JavaFX (FXML + CSS) and uses SQLite for local persistence.

## Key Features

### Authentication and Role Routing

- Login and registration user flows.
- Session-based access control and role-aware navigation.
- Dedicated dashboards for patient, doctor, and admin roles.

### Patient Workflows

- Dashboard home and section navigation.
- Glucose entries (insert/edit/list).
- Symptom entries (insert/edit/list).
- Medication-related views and tracking.

### Doctor Workflows

- Doctor dashboard and patient-oriented sections.
- Patient list and profile-driven interactions.
- Medication insertion/editing flows.

### Admin Workflows

- Admin dashboard and management pages.
- User insertion and administrative control views.

## Technology Stack

From `pom.xml` and project sources:

- **Language:** Java
- **UI:** JavaFX 21.0.8 (`javafx-controls`, `javafx-fxml`, `javafx-graphics`, `javafx-base`)
- **Database:** SQLite (`sqlite-jdbc` 3.43.2.2)
- **Serialization/Utility:** Gson 2.10.1
- **Build Tool:** Maven
- **Testing:** JUnit Jupiter 5.9.3
- **Compiler target:** Java 16 (`maven-compiler-plugin` source/target set to 16)

## Repository Structure

```text
src/
  main/
    java/it/glucotrack/
      Main.java
      component/
      controller/
      model/
      util/
      view/
    resources/
      assets/
        css/
        fxml/
        icons/
        Images/
      database/
        glucotrack_db.sqlite
        Schema.sql
  test/
    java/it/glucotrack/
      ControllerTest.java
      DaoTest.java
      ModelTest.java
    resources/database/
      SchemaTest.sql
```

## Getting Started

### Prerequisites

- JDK compatible with the project compiler target (`16+`)
- Maven 3.8+
- JavaFX runtime configured in your IDE or local environment

### Build

```bash
mvn clean package
```

### Run

Primary entry point:

- `it.glucotrack.Main`

Typical development workflow is to run the app from an IDE with JavaFX runtime parameters configured.

## Running Tests

```bash
mvn test
```

Test classes are located in `src/test/java/it/glucotrack/`.

## Database

The application uses SQLite with schema and DB files under `src/main/resources/database/`.

- Schema: `src/main/resources/database/Schema.sql`
- Default DB path used in code: `src/main/resources/database/glucotrack_db.sqlite`

Main schema tables include:

- `users`
- `glucose_measurements`
- `medications`
- `log_medications`
- `patient_symptoms`
- `risk_factors`
- `medication_edits`

## UI and Navigation

- FXML views are stored in `src/main/resources/assets/fxml/`.
- CSS styles are stored in `src/main/resources/assets/css/`.
- `Main` initializes the database, sets app icon/stage defaults, and navigates to the login view through the navigator service.

## Architecture Notes

The codebase follows an MVC-style organization and uses utility/service layers for data access and navigation.

- `model`: domain entities and business objects
- `controller`: JavaFX controllers and UI logic
- `util`: database/session/helpers
- `view`: navigation/view orchestration helpers

Project documentation (`Relazione.md`) references additional patterns and design rationale used during development.

## Contributing

1. Create a dedicated feature branch.
2. Keep commits small, focused, and descriptive.
3. Run tests before opening a pull request.
4. Include a concise summary of changed controllers/views/resources.

