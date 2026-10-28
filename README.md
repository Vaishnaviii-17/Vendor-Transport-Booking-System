# Vendor Transport Booking System

**DevOps Mini-Project | Ansible-Provisioned Vendor Transport Booking System**

A Java/Spring Boot transport-booking application used to demonstrate source control, automated builds and tests, Docker packaging, Jenkins CI/CD, Ansible-managed deployment, health verification, idempotency, and failure recovery.

> **Project status:** Tasks 1–14 have been implemented and locally tested as described in the project workflow. Task 15 covers final documentation and submission evidence. Application-specific functionality should be checked against the current source code before making additional feature claims.

## 1. Project overview

The system is intended to support transport availability, booking requests, booking-status tracking, and cancellation, with administrative handling of transport resources and requests. The repository's initial documentation identifies `User`, `Vehicle`, `TransportSlot`, and `Booking` entities. The uploaded deployment and build files establish the DevOps implementation; they do not by themselves prove the full set of business endpoints or UI screens.

## 2. Technology stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Application | Spring Boot 4.1.1, Spring MVC, Thymeleaf |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate |
| Security | Spring Security, OAuth2 Resource Server dependency |
| Build | Maven 3.9.16 |
| Testing | Maven test lifecycle, Spring Boot test dependencies, Selenium 4.27.0 |
| CI/CD | Jenkins declarative pipeline |
| Packaging | Docker, Eclipse Temurin 21 JRE |
| Deployment | Ansible with `community.docker` |
| Version control | Git, GitHub, feature branches and pull requests |
| Monitoring | Spring Boot Actuator `/actuator/health` |

## 3. Architecture and ports

```text
GitHub repository
       |
       v
Jenkins (Checkout -> Build -> Backend Tests -> Application Check
         -> Selenium Tests -> Package -> Docker Build -> Docker Deploy)
       |
       v
Docker: vendor-transport-app (host 8084 -> container 8083)

Ansible (independent deployment workflow)
       |
       +--> Candidate: vendor-transport-ansible-candidate (8086 -> 8083)
       |         |
       |         +--> Health validation
       |
       +--> Production: vendor-transport-ansible (8085 -> 8083)
                 |
                 +--> Previous container backup and rollback on failure

Both application deployments -> PostgreSQL on host.docker.internal:5432
```

**Important:** The current Jenkinsfile deploys its own Docker container. It does **not** invoke the Ansible playbook; Jenkins and Ansible are separate deployment workflows.

| Service | Local endpoint |
| --- | --- |
| Direct Spring Boot application | `http://localhost:8083` |
| Jenkins-managed Docker deployment | `http://localhost:8084` |
| Ansible-managed Docker deployment | `http://localhost:8085` |
| Temporary Ansible candidate | `http://localhost:8086` |
| Ansible deployment health | `http://localhost:8085/actuator/health` |

## 4. Prerequisites

- Java 21 and Maven 3.9.16
- PostgreSQL with database `vendor_transport_db` and an authorized application user
- Docker Desktop with a working Docker daemon
- Jenkins with tools configured as `JDK-21` and `Maven-3.9.16`
- Ansible and the `community.docker` collection for Ansible deployment
- Python Docker SDK in the Python environment used by Ansible
- Git and a terminal (Windows PowerShell for the Windows-hosted Jenkins workflow; WSL Ubuntu for the documented Ansible workflow)

The Jenkinsfile currently references a **machine-specific Docker executable path**. Update `DOCKER_EXE` if Jenkins runs on another machine. It also assumes the application is reachable at `http://localhost:8083` during its **Check Application** stage; ensure the local app is running before triggering that pipeline, or adapt the stage to your environment.

## 5. Local application setup

Create `vendor_transport_db` in PostgreSQL and configure the application's database connection. In the terminal session used to launch the application, supply the required environment variables without committing their values:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/vendor_transport_db'
$env:DB_USERNAME = '<database-user>'
$env:DB_PASSWORD = '<database-password>'
$env:JWT_SECRET = '<application-jwt-secret>'

mvn clean package -DskipTests
mvn spring-boot:run
```

The application's configured port in the provided project documentation and Dockerfile is `8083`. Check readiness at:

```powershell
Invoke-RestMethod http://localhost:8083/actuator/health
```

For automated tests, use `mvn test` with the necessary database, secret, browser, and application prerequisites. Selenium tests may require an available browser and a running application.

## 6. Jenkins CI/CD pipeline

The repository's `Jenkinsfile` defines the following stages:

1. **Checkout:** Fetch source from the configured SCM.
2. **Build:** Run Maven compilation with tests skipped.
3. **Backend Tests:** Execute tests excluding `VendorTransportSeleniumTest`.
4. **Check Application:** Poll `http://localhost:8083` for a successful response.
5. **Selenium Tests:** Execute `VendorTransportSeleniumTest` in headless mode.
6. **Package:** Build the executable JAR.
7. **Docker Build:** Build image `vendor-transport-booking:<Jenkins BUILD_NUMBER>`.
8. **Docker Deploy and Verify:** Deploy `vendor-transport-app` on port `8084` and poll Actuator for `UP`.

Jenkins uses credential IDs `vendor-jwt-secret`, `vendor-db-username`, and `vendor-db-password`. Create these as **Secret text** credentials in Jenkins; do not store secret values in source control. The pipeline publishes JUnit reports, optionally archives Selenium screenshots, and archives the JAR on success.

When an earlier Jenkins-managed container exists, the pipeline stops and renames it as a backup, then deploys the replacement. On a deployment exception, it attempts to remove the failed container and restart the backup. This is a best-effort rollback; examine Jenkins logs to confirm recovery succeeded.

## 7. Docker packaging

The `Dockerfile` uses `eclipse-temurin:21-jre`, copies `target/vendor-transport-booking-0.0.1-SNAPSHOT.jar` to `/app/app.jar`, exposes port `8083`, and runs the JAR with `java -jar`.

```powershell
mvn -DskipTests package
docker build -t vendor-transport-booking:local .
```

The image must be built before running the Ansible playbook with that tag. The currently configured Ansible image tag is `7`.

## 8. Ansible deployment

The playbook is `ansible/deploy.yml`; the local inventory is `ansible/inventory.ini` and settings are in `ansible/group_vars/all.yml`.

The deployment workflow:

1. Validates required secrets and Docker connectivity.
2. Confirms the requested Docker image exists locally.
3. Inspects the current production and backup containers.
4. Checks image, selected environment settings, port mapping, and application health to determine whether a change is needed.
5. Starts a candidate on port `8086` and waits for health `UP`.
6. If not in candidate-only mode, preserves the old production container, starts the replacement on `8085`, and checks health.
7. Removes the backup after success or attempts rollback after failure.
8. Removes the temporary candidate in the `always` handler.

### Run from WSL Ubuntu

```bash
cd /mnt/c/Sem-7/DevOpps/Project/Vendor-Transport-Booking-System/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

# DB_USERNAME, DB_PASSWORD and JWT_SECRET must already be set securely.
ansible-playbook --syntax-check deploy.yml
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  deploy.yml
```

The Python interpreter path above matches the tested local setup; adjust it for other installations.

### Candidate-only validation

```bash
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  -e "force_candidate_test=true" \
  deploy.yml
```

Candidate-only mode validates the image on port `8086` without replacing production.

### Rollback simulation — disruptive test

**Warning:** This test temporarily stops and replaces the Ansible-managed production container on port `8085`. Run only in an approved test environment or maintenance window.

```bash
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  -e "simulate_production_failure=true" \
  deploy.yml
```

The simulation deliberately fails after creating a replacement production container. A final Ansible `failed=1` is **expected** for this test; successful rollback is confirmed separately by the original container ID, running state, and Actuator health.

### Change the deployment image

Update `image_tag` in `ansible/group_vars/all.yml` to the tag of a **locally available, tested** Docker image. The Jenkins pipeline produces build-number tags, while the Ansible configuration currently targets tag `7`; there is no automatic tag handoff between these workflows.

## 9. Validation and test evidence

The following results were observed in local Task 14 execution:

| Scenario | Observed result |
| --- | --- |
| Missing Docker image | Rejected before production changes |
| Unchanged healthy deployment | `changed=0`, `failed=0` |
| Candidate failure | Entered rescue and removed candidate |
| Successful candidate-only validation | Candidate healthy, removed after test; production unchanged |
| Simulated production failure | Existing container restored from backup |
| Post-rollback health | `status: UP` |
| Post-rollback idempotency | `ok=11`, `changed=0`, `failed=0`, `skipped=13` |

The verified rollback restored the **same original container ID**, confirming that the previous container—not merely an equivalent replacement—was restarted. Preserve screenshots or logs for the final academic submission; this README is a summary, not a substitute for evidence artifacts.

Useful verification commands:

```bash
docker ps --filter 'name=vendor-transport'
curl -fsS http://localhost:8084/actuator/health
curl -fsS http://localhost:8085/actuator/health
docker ps -a --filter 'name=vendor-transport-ansible-candidate'
docker ps -a --filter 'name=vendor-transport-ansible-backup'
```

## 10. Branching and collaboration

- `main`: stable baseline
- `develop`: integration branch
- `feature/*`: feature implementation
- `bugfix/*`: defect fixes
- `hotfix/*`: urgent corrections

Create a dedicated branch for each task, open a pull request, review changes, and merge into `develop`. Task 15 documentation should be developed on `docs/final-project-documentation`.

## 11. Security and operational notes

- Do not commit database passwords, JWT secrets, `.env` files, or Jenkins credential values.
- The Ansible playbook reads secrets from the execution environment and suppresses sensitive task output using `no_log` where configured.
- The Jenkins pipeline writes a temporary Docker environment file and removes it in `finally`; ensure Jenkins workspace access is restricted.
- The current `community.docker` collection emits a compatibility warning with Ansible 2.16.3 in the tested environment. The reported tests succeeded, but compatible versions should be selected for future maintenance.
- Health checks demonstrate application readiness, not complete end-to-end business correctness.
- The current deployment process is designed for a local Docker Desktop/WSL lab environment; additional hardening would be required for production hosting.

## 12. Submission checklist

- [ ] Final README reviewed against the repository
- [ ] Architecture and CI/CD diagrams added
- [ ] Jenkins successful build and test reports captured
- [ ] Selenium test evidence captured
- [ ] Docker build/container and Actuator health screenshots captured
- [ ] Ansible idempotency, candidate, and rollback evidence captured
- [ ] Final project report prepared
- [ ] Task 15 documentation pull request reviewed and merged into `develop`

## Repository

[Vendor Transport Booking System on GitHub](https://github.com/Vaishnaviii-17/Vendor-Transport-Booking-System)
