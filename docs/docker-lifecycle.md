
## Troubleshooting log: container exited on first run
- Symptom: `docker ps -a` showed `Exited (1)`; `curl` returned HTTP 000.
- Diagnosis: `docker logs` showed `Access denied for user ... (using password: YES)`, so the network path to MySQL worked but the credentials in `docker.env` were wrong.
- Lesson: an env file is read only when a container is created. Restarting from Docker Desktop re-used the old settings, so the container had to be removed and recreated with `docker rm -f` and `docker run`.
- Fix: corrected `DB_USERNAME` / `DB_PASSWORD` in `docker.env` and recreated the container. Result: HTTP 200.
