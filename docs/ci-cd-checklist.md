# Checklist setup CI/CD

Checklist cho repo `ticketbooking`. `[x]` là đã có trong repo, `[ ]` là còn phải làm.

Thứ tự nên làm: **2 → 4 → 5**. Chưa có actuator và migration DB thì chưa làm CD an toàn được.

## 1. Các workflow CI đã có (`.github/workflows/`)

- [x] **CI** (`ci.yml`): build, unit test, báo cáo JaCoCo, integration test bằng Testcontainers, Dependency Review, báo Telegram khi `main` bị lỗi
- [x] **CodeQL** (`codeql.yml`): quét Java khi có PR, push vào `main` và hằng tuần
- [x] **Secret Scan** (`secret-scan.yml`): gitleaks, upload kết quả SARIF
- [x] **PR Title** (`pr-title.yml`): kiểm tra tên PR theo Conventional Commits
- [x] **Docker** (`docker.yml`): PR chỉ build; `main` và tag `v*` thì đẩy lên GHCR kèm provenance attestation
- [x] **Dependabot** (`dependabot.yml`): cập nhật Maven, GitHub Actions và base image Docker
- [x] Xoá nhánh sau khi merge (`delete-merged-branch.yml`), báo Telegram (`telegram-notify.yml`)

## 2. Việc phải làm tay trong GitHub Settings

- [ ] **Secrets**: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `TELEGRAM_THREAD_ID` (tuỳ chọn). Nếu thiếu thì workflow chỉ báo warning rồi bỏ qua.
- [ ] **Actions → Workflow permissions**: để mặc định read-only (các workflow đã tự khai báo `permissions`)
- [ ] **Code security**: bật Dependency graph (`dependency-review-action` cần cái này), Code scanning và Dependabot alerts
- [ ] **Branch protection / Ruleset cho `main`**:
  - Bắt buộc PR, cấm force push
  - Các check bắt buộc: `Build & Test`, `Integration Test`, `Dependency Review`, `CodeQL (java)`, `Gitleaks`, `Conventional title`, `Build image`
  - Tên check phải khớp đúng với `name:` của job trong workflow
- [ ] **GHCR package**: sau lần push đầu tiên, vào Packages để chọn visibility (public hoặc private) và liên kết package với repo
- [ ] Kiểm tra image `ghcr.io/kitkas1412/ticketbooking:latest` đã có sau khi merge vào `main`

## 3. Phần CI còn nên bổ sung

- [ ] **Quét lỗ hổng image** (Trivy hoặc Grype) trong `docker.yml`, cho fail nếu có lỗi HIGH/CRITICAL. Hiện tại chỉ quét dependency Maven.
- [ ] **Smoke test image**: chạy container vừa build cùng Postgres, Redis và RabbitMQ, rồi gọi thử một endpoint
- [ ] **Ngưỡng coverage tối thiểu** cho JaCoCo (hiện chưa đặt ngưỡng)
- [ ] Chạy load test k6 (`benchmark/k6/flash-sale.js` + `verify.sh`) thủ công bằng `workflow_dispatch` hoặc theo lịch. Cần sửa các import k6 đã cũ trước khi chạy.
- [ ] Workflow **Release**: khi push tag `v*` thì tạo GitHub Release và changelog từ các conventional commit

## 4. Chuẩn bị app trước khi deploy

- [ ] **Thêm `spring-boot-starter-actuator`** cho health check (`/actuator/health`, readiness và liveness)
- [ ] **Chuyển sang migration (Flyway hoặc Liquibase)** và tắt `ddl-auto: update` trên prod. Ba instance cùng chạy `ddl-auto: update` lúc khởi động là rủi ro.
- [ ] Tạo **profile `prod`**: `show-sql: false`, bỏ `format_sql`, chỉnh lại mức log
- [ ] Thêm `HEALTHCHECK` và graceful shutdown (`server.shutdown: graceful`)
- [ ] Làm file **compose cho prod**:
  - Dùng `image: ghcr.io/kitkas1412/ticketbooking:<tag>` thay cho `build: ..`
  - Bỏ các port public của Postgres, Redis và RabbitMQ (`5432`, `6379`, `5672`, `15672`)
  - Thêm `depends_on: condition: service_healthy`
  - Thêm `restart` cho `app1`, `app2`, `app3`
- [ ] Đặt mật khẩu cho Redis (hiện đang không có auth)
- [ ] Nginx: thêm HTTPS/TLS, `proxy_next_upstream`, và health check upstream

## 5. Pipeline CD

- [ ] Chọn nơi deploy (VPS + Docker Compose, Kubernetes, Cloud Run…) và làm workflow `deploy.yml`:
  - `main` → staging tự động; tag `v*` → production
  - Dùng **GitHub Environments** (`staging`, `production`), bật required reviewers cho production
  - Deploy theo **tag cụ thể hoặc digest**, không dùng `latest`
- [ ] **Secrets theo từng environment**:
  - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASS`
  - `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`
  - `JWT_SECRET` (sinh bằng `openssl rand -base64 64`)
  - SSH key hoặc cloud credentials (nên dùng OIDC thay cho key để lâu dài)
- [ ] Server cần login được vào GHCR (PAT có quyền `read:packages`, hoặc để package ở chế độ public)
- [ ] Thứ tự deploy: chạy migration DB → rolling update từng `app1`, `app2`, `app3` → kiểm tra health → smoke test
- [ ] **Rollback**: giữ lại tag hoặc digest của lần deploy trước, có một lệnh hoặc workflow để quay về
- [ ] Tuỳ chọn: xác minh provenance attestation trước khi deploy (`gh attestation verify`)
- [ ] Báo Telegram khi deploy thành công hoặc thất bại (dùng lại format jq trong `telegram-notify.yml`)

## 6. Vận hành sau khi deploy

- [ ] Backup PostgreSQL định kỳ và đã thử restore
- [ ] Theo dõi log, metrics (Actuator + Prometheus) và alert
- [ ] Cập nhật README: badge CI, cách deploy và cách rollback
