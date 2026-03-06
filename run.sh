#!/usr/bin/env bash
# ─────────────────────────────────────────────
#  Quick commands — copy & run what you need
# ─────────────────────────────────────────────

# Start infrastructure (Kafka + Zookeeper + Kafka UI)
docker compose -f docker-compose.infra.yml up -d

# Start application (build + run)
docker compose -f docker-compose.app.yml up -d --build

# ── Or start everything at once ──────────────
# docker compose -f docker-compose.infra.yml -f docker-compose.app.yml up -d --build

# ── Stop ─────────────────────────────────────
# docker compose -f docker-compose.infra.yml down
# docker compose -f docker-compose.app.yml down

# ── Logs ─────────────────────────────────────
# docker compose -f docker-compose.infra.yml logs -f
# docker compose -f docker-compose.app.yml logs -f

# ── Rebuild app only ─────────────────────────
# docker compose -f docker-compose.app.yml up -d --build --force-recreate
