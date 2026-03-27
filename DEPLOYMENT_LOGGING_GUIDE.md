# Deployment and Logging Guide

Complete guide for deploying the Resource Document Backend with comprehensive logging to monitor all operations and data.

## Table of Contents
1. [Logging Overview](#logging-overview)
2. [Docker Deployment](#docker-deployment)
3. [JAR Deployment](#jar-deployment)
4. [Log File Structure](#log-file-structure)
5. [Monitoring and Maintenance](#monitoring-and-maintenance)
6. [Troubleshooting](#troubleshooting)

---

## Logging Overview

### Log Storage Location
All application logs are stored in: **`/DATA/resource_center`**

### Log Files Generated

| File | Purpose | Details |
|------|---------|---------|
| `resource_document-app-YYYY-MM-DD.log` | Application logs | All INFO and above level logs |
| `resource_document-error-YYYY-MM-DD.log` | Error logs | ERROR and FATAL level logs only |
| `resource_document-performance-YYYY-MM-DD.log` | Performance metrics | API, Database, Business Logic duration |
| `resource_document-security-YYYY-MM-DD.log` | Security events | Authentication, Authorization, Security warnings |
| `archive/` | Compressed archives | Auto-compressed daily logs older than 30 days |

### Log Details Included

Each log entry contains:
- **Timestamp**: `yyyy-MM-dd HH:mm:ss.SSS`
- **Thread ID**: Which thread processed the request
- **Log Level**: INFO, WARN, ERROR, DEBUG
- **Logger Name**: Package/class that generated the log
- **Method & Line Number**: Exact code location
- **Message**: Detailed information about the operation

Example:
```
2026-03-27 14:32:45.123 [http-nio-8080-exec-1] INFO com.emenu.service.UserService - [createUser:156] - User created: user-123
```

---

## Docker Deployment

### Prerequisites
- Docker and Docker Compose installed
- `/DATA/resource_center` directory created on host server
- Proper permissions on `/DATA/resource_center` (read/write for Docker)

### Setup Steps

#### 1. Create Log Directory on Host
```bash
# On server at: root@ciftp-db-uat:/DATA/resource_center
sudo mkdir -p /DATA/resource_center/archive
sudo chown -R 1000:1000 /DATA/resource_center  # Adjust UID if needed
sudo chmod 755 /DATA/resource_center
sudo chmod 755 /DATA/resource_center/archive
```

#### 2. Deploy with Docker Compose

**Build and start the application:**
```bash
cd /home/user/resourse_document_backend
docker compose -f docker-compose.app.yml up -d --build
```

**Verify the container is running:**
```bash
docker ps
# Should show: resource-service container running on port 6060
```

**Check logs in real-time:**
```bash
docker logs -f resource-service
```

#### 3. Verify Logging Setup

Check that log files are created:
```bash
ls -lah /DATA/resource_center/
# Should show: resource_document-app-YYYY-MM-DD.log, error log, security log, etc.
```

Monitor log file growth:
```bash
watch -n 5 'ls -lh /DATA/resource_center/*.log'
```

### Docker Environment Variables

The docker-compose configuration sets these automatically:
```yaml
environment:
  LOG_PATH: /DATA/resource_center
  SPRING_PROFILES_ACTIVE: prod
```

### Docker Volume Mounts

```yaml
volumes:
  - /DATA/resource_center:/DATA/resource_center  # Host to Container log mapping
```

---

## JAR Deployment

Deploy directly on server without Docker.

### Prerequisites
- Java 21+ installed
- PostgreSQL, Redis, Kafka running and accessible
- `/DATA/resource_center` directory with proper permissions

### Build Steps

#### 1. Build JAR File

From the repository directory:
```bash
cd /home/user/resourse_document_backend
mvn clean package -DskipTests -P prod
```

**Output**: `target/cambodia-emenu-platform-1.0.0.jar`

#### 2. Prepare Deployment Directory

```bash
# Create deployment directory
mkdir -p /opt/resource-service
cd /opt/resource-service

# Copy JAR
cp /home/user/resourse_document_backend/target/cambodia-emenu-platform-1.0.0.jar app.jar

# Create log directory
mkdir -p /DATA/resource_center/archive
chmod 755 /DATA/resource_center
chmod 755 /DATA/resource_center/archive
```

#### 3. Run JAR with Logging Configuration

**Basic startup:**
```bash
cd /opt/resource-service
java -jar app.jar --spring.profiles.active=prod
```

**With memory optimization:**
```bash
java -Xms512m -Xmx2g \
     -Dlogging.config=classpath:logback-spring.xml \
     -DLOG_PATH=/DATA/resource_center \
     -jar app.jar \
     --spring.profiles.active=prod
```

**With performance tuning:**
```bash
java -Xms1g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Dlogging.config=classpath:logback-spring.xml \
     -DLOG_PATH=/DATA/resource_center \
     -jar app.jar \
     --spring.profiles.active=prod
```

**Background execution (nohup):**
```bash
nohup java -jar app.jar --spring.profiles.active=prod > /DATA/resource_center/startup.log 2>&1 &
echo $! > /opt/resource-service/app.pid
```

**Background execution (systemd - Recommended):**

Create `/etc/systemd/system/resource-service.service`:
```ini
[Unit]
Description=Resource Document Backend Service
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/resource-service
Environment="LOG_PATH=/DATA/resource_center"
Environment="SPRING_PROFILES_ACTIVE=prod"
ExecStart=/usr/bin/java -Xms512m -Xmx2g \
    -Dlogging.config=classpath:logback-spring.xml \
    -DLOG_PATH=/DATA/resource_center \
    -jar app.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Start service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable resource-service
sudo systemctl start resource-service
sudo systemctl status resource-service
```

#### 4. Verify JAR Deployment

Check if service is running:
```bash
curl -s http://localhost:6060/actuator/health | jq .
```

Check logs:
```bash
tail -f /DATA/resource_center/resource_document-app-*.log
tail -f /DATA/resource_center/resource_document-error-*.log
```

---

## Log File Structure

### Directory Layout
```
/DATA/resource_center/
├── resource_document-app-2026-03-27.log         # Application logs (latest)
├── resource_document-error-2026-03-27.log       # Error logs (latest)
├── resource_document-performance-2026-03-27.log # Performance logs (latest)
├── resource_document-security-2026-03-27.log    # Security logs (latest)
└── archive/                                      # Compressed logs (older than 30 days)
    ├── resource_document-app-2026-03-20-1.log.gz
    ├── resource_document-error-2026-03-20-1.log.gz
    ├── resource_document-performance-2026-03-20-1.log.gz
    └── resource_document-security-2026-03-20-1.log.gz
```

### Log Rolling Policies

- **Max File Size**: 512MB per file
- **Max History**: 30 days
- **Total Cap**: 10GB (all log files combined)
- **Compression**: Auto-compressed when archived
- **Archive**: Old logs moved to `archive/` subdirectory

---

## Monitoring and Maintenance

### Real-Time Monitoring

**Watch log file in real-time:**
```bash
tail -f /DATA/resource_center/resource_document-app-$(date +%Y-%m-%d).log
```

**Monitor specific log types:**
```bash
# Error logs
tail -f /DATA/resource_center/resource_document-error-*.log

# Performance logs
tail -f /DATA/resource_center/resource_document-performance-*.log

# Security logs
tail -f /DATA/resource_center/resource_document-security-*.log
```

**Search logs:**
```bash
# Find ERROR entries
grep "ERROR" /DATA/resource_center/resource_document-app-*.log

# Find slow queries (> 1000ms)
grep "SLOW" /DATA/resource_center/resource_document-performance-*.log

# Find authorization denials
grep "DENIED" /DATA/resource_center/resource_document-security-*.log
```

### Disk Space Management

**Check disk usage:**
```bash
du -sh /DATA/resource_center/
ls -lh /DATA/resource_center/ | grep -E "^-"
```

**Clean old archived logs (older than 60 days):**
```bash
find /DATA/resource_center/archive/ -name "*.log.gz" -mtime +60 -delete
```

**Compress current logs manually:**
```bash
cd /DATA/resource_center
gzip *.log  # Compress all current logs
mv *.gz archive/  # Move to archive
```

### Log Rotation Verification

Check if logs are being rotated properly:
```bash
ls -lth /DATA/resource_center/*.log | head -5
```

### Application Health Check

**Health endpoint:**
```bash
curl http://localhost:6060/actuator/health
```

**Metrics endpoint:**
```bash
curl http://localhost:6060/actuator/metrics
```

**Database connection pool metrics:**
```bash
curl http://localhost:6060/actuator/metrics/hikaricp.connections
```

---

## Troubleshooting

### Issue: Logs not being written to /DATA/resource_center

**Solution:**
1. Check permissions:
   ```bash
   ls -ld /DATA/resource_center
   # Should be: drwxr-xr-x (755 or similar with write permission)
   ```

2. Verify LOG_PATH environment variable:
   ```bash
   # Docker
   docker exec resource-service env | grep LOG_PATH

   # JAR
   ps aux | grep java | grep LOG_PATH
   ```

3. Check container/application logs:
   ```bash
   # Docker
   docker logs resource-service | grep -i "log\|error"

   # Systemd
   journalctl -u resource-service -n 50
   ```

### Issue: Logs are growing too fast / disk full

**Check log size:**
```bash
du -sh /DATA/resource_center/
find /DATA/resource_center -name "*.log" -exec du -h {} + | sort -rh
```

**Solutions:**
1. Reduce log level in `application-prod.yaml`:
   ```yaml
   logging:
     level:
       root: WARN
       com.emenu: INFO
   ```

2. Increase max file size in `logback-spring.xml`:
   ```xml
   <property name="MAX_FILE_SIZE" value="1GB" />
   ```

3. Reduce max history:
   ```xml
   <property name="MAX_HISTORY_DAYS" value="15" />
   ```

4. Manually clean old logs:
   ```bash
   find /DATA/resource_center/archive -name "*.log.gz" -mtime +15 -delete
   ```

### Issue: Performance degradation

**Check slow queries:**
```bash
grep "SLOW\|VERY_SLOW" /DATA/resource_center/resource_document-performance-*.log
```

**Check thread usage:**
```bash
grep "http-nio-8080" /DATA/resource_center/resource_document-app-*.log | wc -l
```

**Check exception frequency:**
```bash
grep "ERROR" /DATA/resource_center/resource_document-error-*.log | wc -l
```

### Issue: Missing correlation IDs in logs

Ensure the logging configuration is active:
```bash
grep "CORRELATION-ID" /DATA/resource_center/resource_document-app-*.log
```

If missing, check that `LoggingConfig` bean is properly initialized:
```bash
grep "LoggingConfig\|CorrelationIdFilter" /DATA/resource_center/resource_document-app-*.log
```

---

## Using Logs in Code

Simply use standard Spring Boot logging with `log.info()`:

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        long start = System.currentTimeMillis();
        log.info("Fetching user: {}", id);

        // ... business logic ...

        long duration = System.currentTimeMillis() - start;
        log.info("User fetched successfully - Duration: {}ms", duration);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        try {
            User user = userService.createUser(request);
            log.info("User created successfully: {}", user.getId());
            return ResponseEntity.status(201).body(user);
        } catch (Exception e) {
            log.error("Failed to create user", e);
            throw e;
        }
    }
}
```

All logs will be automatically captured and stored in `/DATA/resource_center/` with proper categorization:
- Application logs: `resource_document-app-YYYY-MM-DD.log`
- Error logs: `resource_document-error-YYYY-MM-DD.log`
- Performance data in logs based on timestamps

---

## Summary

| Aspect | Docker | JAR |
|--------|--------|-----|
| Build | `docker-compose -f docker-compose.app.yml up -d --build` | `mvn clean package -P prod` |
| Logs Location | `/DATA/resource_center` (mounted) | `/DATA/resource_center` |
| Startup | Container auto-restart | Systemd or manual |
| Health Check | `docker logs resource-service` | `curl http://localhost:6060/actuator/health` |
| Stop | `docker compose down` | `systemctl stop resource-service` |

Both methods store logs in `/DATA/resource_center` for easy centralized monitoring and analysis.
