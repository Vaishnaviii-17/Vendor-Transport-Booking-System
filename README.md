# Vendor Transport Booking System

An Ansible-Provisioned Vendor Transport Booking System developed as a DevOps mini-project.

## Overview

The application is designed to allow customers to view available transport slots, request bookings, track booking status, and cancel eligible bookings. Administrators will be able to manage transport availability and confirm or reject requests.

## Technology Stack

- Java 21
- Spring Boot 4.1.1
- Maven 3.9.16
- PostgreSQL 18.4
- Spring Data JPA / Hibernate
- Spring Security
- Embedded Apache Tomcat
- Git and GitHub
- Jenkins, Selenium, Docker, and Ansible (planned integration)

## Current Implementation

- Spring Boot application initialized
- PostgreSQL database connection established
- User, Vehicle, TransportSlot, and Booking entities created
- Spring Data JPA repositories configured
- Initial Spring Security configuration
- REST API test endpoint
- Spring Boot Actuator health endpoint

The complete booking workflows are planned for subsequent development tasks.

## Local Setup

### Prerequisites

Java 21, Maven, PostgreSQL 18, and Git.

### Database

Create a PostgreSQL database named `vendor_transport_db` with an authorized database user.

### Configuration

Set the environment variables `DB_USERNAME` and `DB_PASSWORD` for your local database credentials.

### Run

```powershell
mvn clean package -DskipTests
mvn spring-boot:run
```

The application runs on port `8083`.

### Verification

- API: http://localhost:8083/api
- Health: http://localhost:8083/actuator/health

## Branching Strategy

- `main`: stable baseline
- `develop`: integration branch
- `feature/*`: new features
- `bugfix/*`: bug fixes
- `hotfix/*`: urgent fixes

All feature branches should be reviewed through pull requests before merging into `develop`.

## Planned DevOps Workflow

GitHub → Jenkins → Maven → Selenium → Docker → Ansible → Deployment and Health Check

## Project Status

Task 3 completed. Task 4 GitHub repository initialization in progress.