#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  run.sh  —  easy control for infrastructure and application
#
#  Usage:
#    ./run.sh infra  start          Start Kafka + Zookeeper + Kafka UI
#    ./run.sh infra  stop           Stop infrastructure
#    ./run.sh infra  logs           Tail infrastructure logs
#
#    ./run.sh app    start          Build & start Spring Boot app
#    ./run.sh app    stop           Stop app container
#    ./run.sh app    restart        Rebuild & restart app
#    ./run.sh app    logs           Tail app logs
#
#    ./run.sh all    start          Start infrastructure then app
#    ./run.sh all    stop           Stop app then infrastructure
#
#    ./run.sh status                Show running containers
# ─────────────────────────────────────────────────────────────

set -euo pipefail

INFRA_FILE="docker-compose.infra.yml"
APP_FILE="docker-compose.app.yml"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

require_docker() {
  command -v docker >/dev/null 2>&1 || error "Docker is not installed or not in PATH"
  docker info >/dev/null 2>&1       || error "Docker daemon is not running"
}

infra_start() {
  info "Starting infrastructure (Zookeeper + Kafka + Kafka UI) ..."
  docker compose -f "$INFRA_FILE" up -d
  success "Infrastructure started"
  echo ""
  echo "  Kafka broker  : localhost:9092"
  echo "  Kafka UI      : http://localhost:8090"
}

infra_stop() {
  info "Stopping infrastructure ..."
  docker compose -f "$INFRA_FILE" down
  success "Infrastructure stopped"
}

infra_logs() {
  docker compose -f "$INFRA_FILE" logs -f --tail=100
}

app_start() {
  info "Building and starting application ..."
  docker compose -f "$APP_FILE" up -d --build
  success "Application started"
  echo ""
  echo "  API           : http://localhost:5000"
  echo "  Swagger UI    : http://localhost:5000/swagger-ui.html"
  echo "  Health        : http://localhost:5000/actuator/health"
}

app_stop() {
  info "Stopping application ..."
  docker compose -f "$APP_FILE" down
  success "Application stopped"
}

app_restart() {
  app_stop
  app_start
}

app_logs() {
  docker compose -f "$APP_FILE" logs -f --tail=200
}

show_status() {
  echo ""
  echo -e "${CYAN}Running containers:${NC}"
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# ─── Entry point ────────────────────────────────────────────
require_docker

TARGET="${1:-help}"
ACTION="${2:-help}"

case "$TARGET" in
  infra)
    case "$ACTION" in
      start)   infra_start ;;
      stop)    infra_stop  ;;
      logs)    infra_logs  ;;
      *)       error "Unknown action '$ACTION'. Use: start | stop | logs" ;;
    esac
    ;;
  app)
    case "$ACTION" in
      start)   app_start   ;;
      stop)    app_stop    ;;
      restart) app_restart ;;
      logs)    app_logs    ;;
      *)       error "Unknown action '$ACTION'. Use: start | stop | restart | logs" ;;
    esac
    ;;
  all)
    case "$ACTION" in
      start)
        infra_start
        info "Waiting 10 s for Kafka to be ready ..."
        sleep 10
        app_start
        show_status
        ;;
      stop)
        app_stop
        infra_stop
        ;;
      *)  error "Unknown action '$ACTION'. Use: start | stop" ;;
    esac
    ;;
  status)
    show_status
    ;;
  *)
    echo ""
    echo "  Usage: ./run.sh <target> <action>"
    echo ""
    echo "  Targets:"
    echo "    infra   start | stop | logs"
    echo "    app     start | stop | restart | logs"
    echo "    all     start | stop"
    echo "    status"
    echo ""
    ;;
esac
