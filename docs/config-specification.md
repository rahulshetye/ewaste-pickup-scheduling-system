# Configuration Specification - E-Waste Pickup Target Node

**Target:** Ubuntu 22.04 VM (Multipass), managed over SSH by Ansible.

| Category | Item | Purpose |
|---|---|---|
| Packages | openjdk-17-jre-headless | Java 17 runtime for the Spring Boot jar |
| | mysql-server | Application database |
| | python3-pymysql | Lets Ansible manage MySQL |
| | curl | Health checks |
| Users | `ewaste` (system, nologin) | Runs the application with least privilege |
| | `ewaste_app` (MySQL) | Database login, privileges on `ewaste_pickup` only |
| Folders | `/opt/ewaste/releases/<version>/` | Versioned releases |
| | `/opt/ewaste/current` (symlink) | Live release; switched for rollback |
| | `/etc/ewaste/` (0750) | Configuration and secrets |
| | `/var/log/ewaste/` | Application log |
| Files | `/etc/ewaste/ewaste.env` (0640) | DB_URL, DB_USERNAME, DB_PASSWORD, SERVER_PORT |
| | `/etc/systemd/system/ewaste.service` | Service definition |
| | `/opt/ewaste/releases/<v>/app.jar` | Application binary |
| Ports | 22/tcp | SSH (management) |
| | 8080/tcp | Application HTTP |
| | 3306/tcp | MySQL (local to the node) |
| Services | `mysql` | Enabled and running |
| | `ewaste` | Enabled, runs the jar, restarts on failure |
| Secrets | Ansible Vault (`group_vars/all/vault.yml`) | DB password never stored in plain text in git |

## Troubleshooting note (first run)
The first execution applied 13 changes but failed the health check: MySQL refused the app's login. The jar built on the developer machine contained `application-local.properties`, which `application.properties` imports via `spring.config.import`. Its literal password overrode the `DB_PASSWORD` variable. Fix: build the release jar without that file, then rerun the playbook. Result: HTTP 200.

**Run:** `cd ansible && ansible-playbook site.yml`
**Verify:** `curl http://<node-ip>:8080/api/slots` returns HTTP 200.
