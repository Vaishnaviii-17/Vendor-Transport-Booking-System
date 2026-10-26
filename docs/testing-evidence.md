# Testing Evidence and Final Submission Checklist

**Project:** Vendor Transport Booking System
**Purpose:** Task 15 — consolidate verified DevOps results and identify remaining screenshot evidence.

> **Evidence policy:** A configured Jenkins stage or Maven dependency is not itself proof that a test passed. This document separates results demonstrated in shared terminal output from checks that still require a screenshot, report, or console log. Do not fabricate screenshots, Jenkins build numbers, Selenium pass counts, or test coverage.

## 1. Verification status

| Area | Evidence available | Status |
| --- | --- | --- |
| Ansible playbook syntax | `ansible-playbook --syntax-check deploy.yml` returned `playbook: deploy.yml` | **Verified** |
| Missing-image rejection | Prior Task 14 run rejected `rollback-test-invalid` without replacing the running application | **Verified in project conversation** |
| Candidate health failure | Invalid candidate URL caused failure, `rescue` ran, `always` removed candidate | **Verified** |
| Candidate health success | Candidate reached `UP`; candidate-only mode skipped all production promotion tasks | **Verified** |
| Production rollback | Simulated failure after replacement creation restored original container and health | **Verified** |
| Post-rollback idempotency | Normal playbook returned `Deployment required: False`, `changed=0`, `failed=0` | **Verified** |
| Jenkins pipeline stages | Eight stages defined in uploaded `Jenkinsfile` | **Configured; attach successful build log** |
| Maven backend tests | `Backend Tests` stage invokes Maven test goal | **Configured; attach Surefire results** |
| Selenium tests | `Selenium Tests` stage invokes `VendorTransportSeleniumTest` headlessly | **Configured; attach Selenium/Surefire results** |
| Docker build/deployment | Docker build and deployment stages exist in `Jenkinsfile` | **Configured; attach Jenkins/Docker proof** |
| Jenkins-managed health | Pipeline checks `http://localhost:8084/actuator/health` | **Configured; capture current response** |

## 2. Recorded Ansible evidence

### 2.1 Controlled candidate failure

Command (run in WSL with required environment variables already set):

```bash
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  -e "force_candidate_test=true" \
  -e "candidate_health_check_url=http://localhost:8086/actuator/invalid-test" \
  -e "health_check_retries=2" \
  -e "health_check_delay=2" \
  deploy.yml
```

Recorded recap:

```text
ok=17 changed=2 failed=1 skipped=8 rescued=1
```

The candidate health check failed with a connection-reset response during the short retry window; this verifies execution of the failure/recovery path, **not** that the invalid endpoint returned HTTP 404. The candidate was removed. A nonzero playbook exit is expected for this deliberate failure.

### 2.2 Successful candidate-only validation

```bash
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  -e "force_candidate_test=true" \
  deploy.yml
```

Recorded recap:

```text
ok=17 changed=2 failed=0 skipped=6 rescued=0
```

The candidate passed health verification; production stop, rename, replacement and backup-removal tasks were skipped. Two initial retry messages occurred while the Spring Boot candidate was starting.

### 2.3 Simulated production failure and rollback

The playbook includes a test-only `simulate_production_failure` flag that fails the promotion after a new production container is created. The prior container was renamed as a backup, restored by the `rescue` block, restarted, and health-checked.

```bash
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  -e "simulate_production_failure=true" \
  deploy.yml
```

Recorded recap:

```text
ok=26 changed=8 failed=1 skipped=4 rescued=1
```

The output included `Automatic rollback completed. Previous production container is healthy.` The intentional deployment failure means `failed=1` is expected, despite successful restoration.

**Post-rollback verification from the user's terminal:**

```text
Container ID: 13ae7b0cf274aa3061291a3d081cb8650d3ca01f89d4cc789eceaf5086bbb442
Image: vendor-transport-booking:7
Running: true
Health: {"groups":["liveness","readiness"],"status":"UP"}
Candidate containers: none
Backup containers: none
```

The original container ID matched the pre-test value, demonstrating restoration of the prior container rather than merely launching another container from the same image.

### 2.4 Final idempotency after rollback

```bash
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  deploy.yml
```

Recorded output:

```text
Candidate-only mode: False. Deployment required: False.
localhost : ok=11 changed=0 unreachable=0 failed=0 skipped=13 rescued=0 ignored=0
```

This is the final confirmation that the restored application matches the declared deployment state and requires no changes.

## 3. Jenkins, Maven, Selenium, and Docker evidence to capture

The uploaded `Jenkinsfile` defines these eight stages, in order:

1. Checkout
2. Build
3. Backend Tests
4. Check Application
5. Selenium Tests
6. Package
7. Docker Build
8. Docker Deploy and Verify

The pipeline uses Maven backend tests excluding `VendorTransportSeleniumTest`, then runs that Selenium test separately in headless mode. It also publishes Surefire XML results from `target/surefire-reports/*.xml`.

**Capture the following from an actual successful build; do not assign a build number until observed:**

| Suggested evidence file | Capture | What it proves |
| --- | --- | --- |
| `docs/screenshots/jenkins-stage-view.png` | Jenkins build/stage view with result and build identifier | Pipeline completion |
| `docs/screenshots/jenkins-console.png` | Console showing stage outcomes, with secrets redacted | Build execution |
| `docs/screenshots/backend-tests.png` | Maven/Surefire summary | Backend tests actually passed |
| `docs/screenshots/selenium-tests.png` | Selenium test report/console result | UI automation result |
| `docs/screenshots/docker-containers.png` | `docker ps` showing named application containers and published ports | Container deployment |
| `docs/screenshots/jenkins-health.png` | Response from `http://localhost:8084/actuator/health` | Jenkins-managed application health |
| `docs/screenshots/ansible-health.png` | Response from `http://localhost:8085/actuator/health` | Ansible-managed application health |
| `docs/screenshots/ansible-rollback.png` | Console showing backup restoration and healthy rollback | Failure recovery |
| `docs/screenshots/ansible-idempotency.png` | Final recap with `changed=0 failed=0` | Idempotency |

These are **suggested screenshot filenames**, not existing artifacts. Only add images that have actually been captured. If you include console screenshots, ensure environment variable values, credentials, JWT secrets and database passwords are not visible.

## 4. Reproducible verification commands

### Windows PowerShell — repository and containers

```powershell
git branch --show-current
git status --short
docker ps --filter "name=vendor-transport"
Invoke-RestMethod http://localhost:8084/actuator/health
Invoke-RestMethod http://localhost:8085/actuator/health
```

### Windows PowerShell — Maven tests

From the repository root, with the local test database and required environment variables configured:

```powershell
mvn -B "-Dtest=*,!VendorTransportSeleniumTest" test
mvn -B "-Dtest=VendorTransportSeleniumTest" "-Dselenium.headless=true" test
```

Selenium requires the target application and browser/driver environment expected by the test class. A successful command result must be recorded before marking these as independently verified.

### WSL Ubuntu — Ansible

```bash
cd /mnt/c/Sem-7/DevOpps/Project/Vendor-Transport-Booking-System/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible-playbook --syntax-check deploy.yml
ansible-playbook \
  -e "ansible_python_interpreter=$HOME/.venvs/ansible-docker/bin/python" \
  deploy.yml
curl -fsS http://localhost:8085/actuator/health
```

Do not rerun the production-failure simulation merely for screenshots unless a brief interruption on port 8085 is acceptable. Existing verified console output can be used as evidence if retained.

## 5. Final submission checklist

- [x] Tasks 1–14 implementation work reported complete; Task 14 rollback validated by terminal output
- [x] Final Ansible idempotency verified (`changed=0`, `failed=0`)
- [x] Architecture and CI/CD documentation drafted
- [x] Ansible deployment and rollback documentation drafted
- [ ] Add this `docs/testing-evidence.md` file to the documentation branch
- [ ] Capture real Jenkins stage view and console output
- [ ] Capture actual backend and Selenium test results
- [ ] Capture Docker and both application health endpoints
- [ ] Add screenshots to `docs/screenshots/` with descriptive names
- [ ] Review README and diagrams against current source and pipeline
- [ ] Confirm no secrets or local credential files are staged
- [ ] Run `git diff --cached --check` after staging all files
- [ ] Commit Task 15 documentation and open PR into `develop`
- [ ] Merge PR after review and verify clean `develop` branch

## 6. Known constraints

- The Jenkinsfile does **not** call Ansible. Jenkins and Ansible deploy independently.
- The candidate validation does not provide zero-downtime production promotion: the old container is stopped before the replacement binds port 8085.
- The installed `community.docker` collection emitted an Ansible 2.16.3 compatibility warning during recorded tests; this did not prevent those tests from completing.
- Jenkins and Selenium pass counts, build IDs, test coverage and screenshot evidence are not established by the uploaded configuration files alone.
- The recorded test environment is local Windows/Docker Desktop/WSL, not a verified production cloud deployment.
