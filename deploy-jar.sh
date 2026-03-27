#!/bin/bash

################################################################################
# Resource Document Backend - JAR Deployment Script
#
# This script builds and deploys the application as a standalone JAR
# with comprehensive logging to /DATA/resource_center
#
# Usage:
#   ./deploy-jar.sh [build|start|stop|restart|logs|status]
#
# Examples:
#   ./deploy-jar.sh build          # Build JAR only
#   ./deploy-jar.sh start          # Build and start service
#   ./deploy-jar.sh restart        # Restart service
#   ./deploy-jar.sh logs           # Tail application logs
#   ./deploy-jar.sh status         # Check service status
################################################################################

set -e

# ═══════════════════════════════════════════════════════════════════════════
# CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_NAME="cambodia-emenu-platform"
VERSION="1.0.0"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"

# Deployment paths
DEPLOY_DIR="/opt/resource-service"
LOGS_DIR="/DATA/resource_center"
ARCHIVE_DIR="${LOGS_DIR}/archive"
SERVICE_NAME="resource-service"

# Java options
JAVA_HOME="${JAVA_HOME:-/usr/bin/java}"
MEMORY_MIN="512m"
MEMORY_MAX="2g"
JAVA_OPTS="-Xms${MEMORY_MIN} -Xmx${MEMORY_MAX} -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ═══════════════════════════════════════════════════════════════════════════
# HELPER FUNCTIONS
# ═══════════════════════════════════════════════════════════════════════════

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ ${1:<56} ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# ═══════════════════════════════════════════════════════════════════════════
# BUILD FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

build_jar() {
    print_header "Building JAR"

    cd "$SCRIPT_DIR"

    print_info "Running Maven clean package..."
    mvn clean package -DskipTests -P prod

    JAR_PATH="target/${JAR_NAME}"

    if [ -f "$JAR_PATH" ]; then
        print_success "JAR built successfully: $JAR_PATH"
        echo "$JAR_PATH"
    else
        print_error "JAR build failed"
        exit 1
    fi
}

# ═══════════════════════════════════════════════════════════════════════════
# SETUP FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

setup_deployment() {
    print_header "Setting up Deployment Environment"

    # Create deployment directory
    print_info "Creating deployment directory: $DEPLOY_DIR"
    sudo mkdir -p "$DEPLOY_DIR"
    sudo chmod 755 "$DEPLOY_DIR"
    print_success "Deployment directory ready"

    # Create log directories
    print_info "Creating log directories..."
    sudo mkdir -p "$LOGS_DIR" "$ARCHIVE_DIR"
    sudo chmod 755 "$LOGS_DIR" "$ARCHIVE_DIR"
    print_success "Log directories ready: $LOGS_DIR"

    # Set permissions for current user if needed
    if [ ! -w "$LOGS_DIR" ]; then
        print_warning "Log directory may require elevated permissions"
        print_info "Consider running: sudo chown -R \$USER:\$USER $LOGS_DIR"
    fi
}

# ═══════════════════════════════════════════════════════════════════════════
# DEPLOY FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

deploy_jar() {
    local jar_path=$1

    print_header "Deploying JAR"

    setup_deployment

    print_info "Copying JAR to deployment directory..."
    sudo cp "$jar_path" "$DEPLOY_DIR/app.jar"
    sudo chmod 644 "$DEPLOY_DIR/app.jar"
    print_success "JAR deployed to $DEPLOY_DIR"

    print_info "Creating systemd service file..."
    create_systemd_service

    print_success "Deployment preparation complete"
}

# ═══════════════════════════════════════════════════════════════════════════
# SYSTEMD SERVICE CREATION
# ═══════════════════════════════════════════════════════════════════════════

create_systemd_service() {
    local service_file="/tmp/resource-service.service"

    cat > "$service_file" << 'EOF'
[Unit]
Description=Resource Document Backend Service
After=network.target
StartLimitIntervalSec=0

[Service]
Type=simple
User=root
WorkingDirectory=/opt/resource-service
Environment="LOG_PATH=/DATA/resource_center"
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="JAVA_HOME=/usr/bin/java"

ExecStart=/usr/bin/java -Xms512m -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Dlogging.config=classpath:logback-spring.xml \
    -DLOG_PATH=/DATA/resource_center \
    -jar app.jar

# Restart policy
Restart=on-failure
RestartSec=10
StartLimitBurst=5

# Resource limits
LimitNOFILE=65535
LimitNPROC=65535

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=resource-service

[Install]
WantedBy=multi-user.target
EOF

    print_info "Installing systemd service..."
    sudo cp "$service_file" "/etc/systemd/system/${SERVICE_NAME}.service"
    sudo systemctl daemon-reload
    print_success "Systemd service installed"
}

# ═══════════════════════════════════════════════════════════════════════════
# START FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

start_service() {
    print_header "Starting Service"

    print_info "Stopping any existing instances..."
    sudo systemctl stop "$SERVICE_NAME" 2>/dev/null || true

    print_info "Starting $SERVICE_NAME service..."
    sudo systemctl start "$SERVICE_NAME"

    sleep 2

    if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
        print_success "Service started successfully"
        print_info "Waiting for application to fully initialize..."
        sleep 5

        # Try health check
        if command -v curl &> /dev/null; then
            print_info "Running health check..."
            if curl -s http://localhost:6060/actuator/health > /dev/null 2>&1; then
                print_success "Application is healthy and ready"
            else
                print_warning "Health check endpoint not yet responding"
                print_info "Check logs: journalctl -u ${SERVICE_NAME} -f"
            fi
        fi
    else
        print_error "Failed to start service"
        print_info "Check logs: journalctl -u ${SERVICE_NAME} -xe"
        exit 1
    fi
}

# ═══════════════════════════════════════════════════════════════════════════
# STOP FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

stop_service() {
    print_header "Stopping Service"

    if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
        print_info "Stopping $SERVICE_NAME service..."
        sudo systemctl stop "$SERVICE_NAME"
        sleep 2
        print_success "Service stopped"
    else
        print_warning "Service is not running"
    fi
}

# ═══════════════════════════════════════════════════════════════════════════
# RESTART FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

restart_service() {
    print_header "Restarting Service"
    stop_service
    start_service
}

# ═══════════════════════════════════════════════════════════════════════════
# STATUS FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

show_status() {
    print_header "Service Status"

    # Service status
    print_info "Systemd service status:"
    sudo systemctl status "$SERVICE_NAME" --no-pager || print_warning "Service not found in systemd"

    echo

    # Process check
    print_info "Process check:"
    if pgrep -f "app.jar" > /dev/null; then
        print_success "Java process is running"
        ps aux | grep "app.jar" | grep -v grep | awk '{print "  PID: " $2 ", Memory: " $6 "KB"}'
    else
        print_error "Java process not running"
    fi

    echo

    # Port check
    print_info "Port status (8080):"
    if netstat -tuln 2>/dev/null | grep -q ":8080 "; then
        print_success "Port 8080 is listening"
    else
        print_warning "Port 8080 is not listening"
    fi

    echo

    # Log directory check
    print_info "Log files:"
    ls -lh "$LOGS_DIR"/resource_document-*.log 2>/dev/null || print_warning "No log files found"
}

# ═══════════════════════════════════════════════════════════════════════════
# LOGS FUNCTION
# ═══════════════════════════════════════════════════════════════════════════

show_logs() {
    local log_type=${1:-app}

    case "$log_type" in
        app)
            print_header "Application Logs"
            tail -f "$LOGS_DIR"/resource_document-app-*.log
            ;;
        error)
            print_header "Error Logs"
            tail -f "$LOGS_DIR"/resource_document-error-*.log
            ;;
        performance)
            print_header "Performance Logs"
            tail -f "$LOGS_DIR"/resource_document-performance-*.log
            ;;
        security)
            print_header "Security Logs"
            tail -f "$LOGS_DIR"/resource_document-security-*.log
            ;;
        system)
            print_header "System Logs (Systemd)"
            sudo journalctl -u "$SERVICE_NAME" -f
            ;;
        *)
            print_error "Unknown log type: $log_type"
            echo "Available: app, error, performance, security, system"
            exit 1
            ;;
    esac
}

# ═══════════════════════════════════════════════════════════════════════════
# MAIN COMMAND HANDLER
# ═══════════════════════════════════════════════════════════════════════════

main() {
    local command="${1:-help}"

    case "$command" in
        build)
            jar_path=$(build_jar)
            echo "$jar_path"
            ;;
        deploy)
            jar_path=$(build_jar)
            deploy_jar "$jar_path"
            ;;
        start)
            jar_path=$(build_jar)
            deploy_jar "$jar_path"
            start_service
            ;;
        stop)
            stop_service
            ;;
        restart)
            restart_service
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "${2:-app}"
            ;;
        help|*)
            cat << 'HELP'
╔════════════════════════════════════════════════════════════╗
║   Resource Document Backend - JAR Deployment Script       ║
╚════════════════════════════════════════════════════════════╝

USAGE:
  ./deploy-jar.sh [COMMAND] [OPTIONS]

COMMANDS:
  build                Build JAR only
  deploy               Build and deploy JAR to /opt/resource-service
  start                Build, deploy, and start service
  stop                 Stop the service
  restart              Restart the service
  status               Show service status
  logs [TYPE]          Tail logs (types: app, error, performance, security, system)
  help                 Show this help message

EXAMPLES:
  ./deploy-jar.sh build
  ./deploy-jar.sh start
  ./deploy-jar.sh restart
  ./deploy-jar.sh logs app
  ./deploy-jar.sh logs error
  ./deploy-jar.sh logs system

CONFIGURATION:
  Deployment Dir:  /opt/resource-service
  Logs Dir:        /DATA/resource_center
  Service Name:    resource-service
  Port:            8080 (mapped to 6060 via proxy)

LOGS LOCATION:
  Application:     /DATA/resource_center/resource_document-app-*.log
  Errors:          /DATA/resource_center/resource_document-error-*.log
  Performance:     /DATA/resource_center/resource_document-performance-*.log
  Security:        /DATA/resource_center/resource_document-security-*.log
  Archive:         /DATA/resource_center/archive/

For more information, see: DEPLOYMENT_LOGGING_GUIDE.md
HELP
            ;;
    esac
}

# ═══════════════════════════════════════════════════════════════════════════
# EXECUTE
# ═══════════════════════════════════════════════════════════════════════════

main "$@"
