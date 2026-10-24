# Troubleshooting

See Step 60 and section H of `docs/SETUP_EVERY_STEP.md` for commands and expected symptoms. Capture `mvn -B -ntp clean test` output and the first root cause, plus `docker compose ps`, `docker compose logs db`, `docker compose logs mock-core`, and Spring Boot terminal logs.
