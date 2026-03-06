# Start infra
docker compose -f docker-compose.infra.yml up -d

# STOP infra
docker compose -f docker-compose.infra.yml down

# Start app (build + run)
docker compose -f docker-compose.app.yml up -d --build

# STOP app
docker compose -f docker-compose.app.yml down

# Everything at once
docker compose -f docker-compose.infra.yml -f docker-compose.app.yml up -d --build

# Stop / Logs / Rebuild — all commented inside run.sh
