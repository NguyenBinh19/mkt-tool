# Hướng dẫn CI/CD cho LeadFlow (GitHub Actions → GHCR → Ubuntu/Docker Compose)

Tài liệu này dành cho repository `NguyenBinh19/mkt-tool`, một server Ubuntu đang vào được qua Tailscale, và ứng dụng tại `stayhubb2b.site`. Đây là **hướng dẫn triển khai**, không phải tuyên bố rằng pipeline đã được bật. Ở thời điểm viết, repo chưa có `.github/workflows/`; `docker-compose.yml` vẫn build image trên server. Hãy làm theo từng giai đoạn và chỉ bật deploy tự động sau khi kiểm tra các điều kiện ở cuối tài liệu.

> Không đưa mật khẩu server, token GitHub, mật khẩu MySQL, cookie/profile Facebook hoặc nội dung `.env` vào Git, log workflow hay chat. Không chạy `docker compose down -v` trên production.

## 1. Mô hình và lý do chọn

```text
Pull request ──→ CI: Maven tests + frontend checks + Docker build check
      │
      └── merge main ──→ build hai image bất biến ──→ GHCR (tag = commit SHA)
                                                   │
                         Run workflow / duyệt ────┘
                                   ↓
                      GitHub-hosted runner + Tailscale
                                   ↓
                    Tailscale SSH → user deploy trên server
                                   ↓
                    root-owned deploy script, backup MySQL
                                   ↓
                     Compose pull → up → smoke test
                                   ↓
                       ghi SHA hoặc rollback image
```

- **CI không có quyền production.** Pull request chỉ test/build; không nhận secret deploy.
- **Build một lần.** Server pull đúng image đã qua CI; không build source trên server ở mỗi lần release.
- **CD có nút bấm và giới hạn nhánh.** Với một server duy nhất, nên deploy có chủ ý thay vì tự động ngay khi merge.
- **Dữ liệu ngoài image.** Named volume MySQL và bind mount profile Facebook tiếp tục được giữ nguyên; deploy không xóa volume.
- **Không có zero-downtime thật** trên Compose một backend. Backend/Chromium sẽ khởi động lại ngắn; job `QUEUED`/`RUNNING` trong DB hiện được chuyển lại thành `QUEUED` sau restart, nhưng người dùng có thể thấy phiên đang xử lý bị gián đoạn.

## 2. Đối chiếu với repo hiện tại

| Thành phần | Hiện có | Cần cho CI/CD |
| --- | --- | --- |
| Backend | Java 21, `mvn verify`, Dockerfile đa giai đoạn | CI chạy test trước khi publish image |
| Frontend | `npm ci`, `npm run test:i18n`, `npm run build` | CI chạy ba lệnh này; bổ sung test UI sau |
| Compose | MySQL + backend + Nginx, có healthcheck | Override production dùng `image:` theo SHA và `--no-build` |
| Database | Volume, Hibernate `ddl-auto: update` | Backup trước deploy và kiểm thử restore định kỳ; kiểm tra schema sau khi khởi động |
| Facebook | Profile mounted, số tab theo `FACEBOOK_MAX_CONCURRENT_TABS` (mặc định 1) | Giữ mount; tránh deploy lúc cao điểm |
| Smoke test | `deploy/smoke-test.sh` đăng nhập admin bootstrap | Thay bằng smoke test không phụ thuộc mật khẩu bootstrap |
| GitHub Actions | Chưa có | Thêm workflow CI/publish/deploy |

**Lưu ý bảo mật:** `DEPLOY.md` hiện mô tả Cloudflare Flexible + origin HTTP. Với ứng dụng có đăng nhập, đây không phải đích production tốt: Cloudflare nói Flexible không mã hóa đoạn Cloudflare → origin. Trước khi mở cho khách hàng, dùng Cloudflare Tunnel hoặc origin HTTPS với Full (strict). Việc đổi mạng/SSL là một hạng mục riêng, không được mặc nhiên coi là đã xong nhờ CI/CD. Xem [Cloudflare Flexible](https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/flexible/).

## 3. Điều kiện trước khi bắt đầu

1. GitHub repository đã có nhánh `main`; bạn có quyền quản trị repository và Tailscale tailnet.
2. Server đã cài Docker Engine + Compose plugin, còn đủ đĩa cho **ít nhất hai phiên bản** backend/frontend image và backup DB.
3. `/opt/leadflow` trên server chứa `docker-compose.yml`, `.env` production và profile Facebook; kiểm tra bằng `docker compose ps` trước khi đổi bất cứ thứ gì.
4. `.env` production phải là bản riêng trên server, quyền `600`; không copy lên GitHub Actions.
5. Có bản backup MySQL **đã thử restore** ở một DB khác. Backup trên cùng server không bảo vệ được khi ổ đĩa/server hỏng; lưu thêm bản mã hóa ngoài server.
6. Bạn có phiên SSH quản trị thứ hai đang hoạt động trước khi sửa policy Tailscale. Đừng khóa mất đường vào duy nhất.
7. Xem lại `.dockerignore` trước khi build ở máy cá nhân: phải loại trừ mọi thư mục profile Facebook (`playwright-profile*`), file `.env` và backup. GitHub-hosted runner chỉ checkout file đã commit, nhưng không nên dựa vào điều đó để bảo vệ Docker build context local.
8. Kiểm tra `git status --short` ở máy phát triển. Repo hiện có nhiều thay đổi chưa commit; hãy review, test, rồi commit/push đúng bản bạn định phát hành trước khi tạo workflow. SHA trên GitHub không bao gồm file chỉ tồn tại ở máy cá nhân.

Kiểm tra ban đầu trên server:

```bash
cd /opt/leadflow
docker compose version
docker compose config --quiet
docker compose ps
df -h
stat -c '%a %U:%G %n' .env playwright-profile
```

Không in nội dung `.env` ra terminal chia sẻ hoặc log CI.

## 4. GitHub: nhánh, package, quyền

1. Trong GitHub, bật branch protection/ruleset cho `main`: yêu cầu pull request và CI xanh trước khi merge. Không cho push thẳng nếu có thể.
2. Trong **Actions → General**, cấp `GITHUB_TOKEN` quyền tối thiểu; job publish sẽ khai báo `packages: write`, job test chỉ `contents: read`.
3. Tạo Environment tên `production`. Nếu gói GitHub của repo hỗ trợ, giới hạn deploy từ `main` và thêm required reviewer. Một số khả năng Environment trên repo private phụ thuộc gói GitHub; nếu không có reviewer, dùng riêng `workflow_dispatch` và giới hạn người có quyền write. Kiểm tra [GitHub Environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments).
4. GHCR sẽ có hai package: `ghcr.io/nguyenbinh19/mkt-tool-backend` và `ghcr.io/nguyenbinh19/mkt-tool-web`. Tên image viết chữ thường. Workflow publish dùng `GITHUB_TOKEN`; không cần PAT ghi package trong GitHub Actions. Xem [GitHub GHCR](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images).
5. Nếu package private, server cần credential **chỉ đọc package** để pull. Tạo PAT classic có `read:packages` cho tài khoản có quyền đọc package. Đăng nhập GHCR **một lần trên server** bằng `docker login ... --password-stdin`; bảo vệ `/root/.docker/config.json`. Không để token trong `.env` ứng dụng. Nếu package public thì không cần bước này.

Ví dụ đăng nhập GHCR trên server, nhập token khi được hỏi (không gõ token trực tiếp vào command):

```bash
read -r -s -p 'GHCR read token: ' GHCR_READ_TOKEN
printf '\n'
printf '%s' "$GHCR_READ_TOKEN" | docker login ghcr.io -u NguyenBinh19 --password-stdin
unset GHCR_READ_TOKEN
```

## 5. Tailscale: cho CI đến đúng server

Máy của bạn hiện SSH bằng Tailscale SSH và từng bị yêu cầu xác minh qua trình duyệt. Job CI không thể bấm xác minh đó, vì vậy phải có **rule riêng cho automation**: nguồn `tag:ci` → server `tag:prod` → chỉ user Linux `deploy`, action `accept`; giữ rule `check` hiện tại cho người quản trị.

Thứ tự an toàn:

1. Trong Tailscale Admin Console, tạo tag `tag:ci` và `tag:prod`. Đặt owner của tag chỉ là admin tailnet.
2. Chỉnh tailnet policy **bằng cách bổ sung**, không thay toàn bộ policy cũ. Cần cả network grant TCP 22 và SSH rule. Ví dụ phần cần bổ sung:

```jsonc
{
  "grants": [
    { "src": ["tag:ci"], "dst": ["tag:prod"], "ip": ["tcp:22"] }
  ],
  "ssh": [
    { "action": "accept", "src": ["tag:ci"], "dst": ["tag:prod"], "users": ["deploy"] }
  ]
}
```

Đây là **mảnh ghép**, không phải toàn bộ policy. Giữ rule cho tài khoản quản trị để vẫn vào được server sau khi gắn `tag:prod`; ví dụ nguồn là email Tailscale của bạn, đích `tag:prod`, user `root`/`deploy`, action `check`. Kiểm tra policy bằng trình kiểm tra trong Tailscale UI **trước** khi lưu. Tagged source chỉ SSH tới tagged destination, và Tailscale SSH cần cả network + SSH permission. Xem [Tailscale SSH](https://tailscale.com/docs/features/tailscale-ssh).

3. Gắn `tag:prod` cho server trong Tailscale Admin Console. Trước khi đóng phiên root cũ, mở **một phiên SSH quản trị mới** để xác nhận quyền vẫn hoạt động.
4. Tạo Tailscale workload identity federation cho GitHub Actions, giới hạn đúng repository/workflow và `tag:ci`. Lưu **Client ID** và **Audience** thành GitHub secrets `TS_OAUTH_CLIENT_ID` và `TS_AUDIENCE`. Cách này không cần auth key sống lâu. Workflow cần `id-token: write`. Xem [Tailscale GitHub Action](https://tailscale.com/docs/integrations/github/github-action) và [workload identity federation](https://tailscale.com/docs/features/workload-identity-federation).

## 6. Server: tài khoản deploy và ranh giới quyền

Không cho CI SSH trực tiếp vào `root`. Tạo Linux user `deploy` **không thuộc nhóm `docker`** (quyền Docker thực tế tương đương root). Chỉ cho user này chạy một script root-owned được kiểm tra tham số:

```bash
sudo useradd --create-home --shell /bin/bash deploy
id deploy
```

Nếu `deploy` đã tồn tại thì không chạy lại lệnh tạo user. Tạo `/usr/local/sbin/leadflow-deploy` theo mẫu ở mục 9, owner `root:root`, mode `755`. File Compose và `.env` trên server cũng chỉ root được sửa. Mở sudoers bằng `visudo`, thêm rule cho đúng **một script**, không cấp `NOPASSWD: ALL`:

```text
deploy ALL=(root) NOPASSWD: /usr/local/sbin/leadflow-deploy *
```

Script bắt buộc kiểm tra argument là **đúng 40 ký tự hex** và không gọi shell với chuỗi đầu vào tùy ý. Test trước bằng phiên quản trị: `sudo -u deploy sudo -n /usr/local/sbin/leadflow-deploy invalid` phải từ chối ngay mà không deploy.

## 7. Compose: dùng image đã publish

Giữ file Compose hiện tại cho local/manual build. Tạo thêm `/opt/leadflow/docker-compose.cd.yml` trên server, root-owned:

```yaml
services:
  backend:
    image: ghcr.io/nguyenbinh19/mkt-tool-backend:${IMAGE_TAG:?IMAGE_TAG is required}
  nginx:
    image: ghcr.io/nguyenbinh19/mkt-tool-web:${IMAGE_TAG:?IMAGE_TAG is required}
```

Lệnh deploy luôn dùng cả hai file và `--no-build`. Như vậy dù file gốc còn `build:`, server không build lại source. Không dùng `:latest` cho release. Giữ nguyên MySQL volume và mount `PLAYWRIGHT_PROFILE_DIRECTORY`.

Kiểm tra cấu hình sau khi hai image đầu tiên đã publish:

```bash
cd /opt/leadflow
export IMAGE_TAG=<SHA-40-KY-TU-CUA-MAIN>
docker compose --env-file .env -f docker-compose.yml -f docker-compose.cd.yml config --quiet
docker compose --env-file .env -f docker-compose.yml -f docker-compose.cd.yml config --images
```

Kết quả phải là MySQL image cùng đúng hai image GHCR mang cùng SHA.

## 8. Workflow CI + publish trong repo

Tạo `.github/workflows/ci.yml`. Mẫu tối thiểu dưới đây chạy test trên PR và `main`; chỉ `main` mới push image. Thay action tag bằng full commit SHA được xác minh trước khi dùng lâu dài, vì GitHub khuyến nghị pin SHA để giảm rủi ro supply chain.

```yaml
name: CI and publish
on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B verify
        working-directory: backend

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
        working-directory: frontend
      - run: npm run test:i18n
        working-directory: frontend
      - run: npm run build
        working-directory: frontend

  image-check:
    if: github.event_name == 'pull_request'
    needs: [backend, frontend]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker compose --env-file .env.example config --quiet
      - run: docker build -t leadflow-backend-ci -f backend/Dockerfile backend
      - run: docker build -t leadflow-web-ci -f frontend/Dockerfile .
      - run: docker run --rm --entrypoint nginx leadflow-web-ci -t

  publish:
    if: github.event_name == 'push'
    needs: [backend, frontend]
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./backend
          file: ./backend/Dockerfile
          push: true
          tags: ghcr.io/nguyenbinh19/mkt-tool-backend:${{ github.sha }}
      - uses: docker/build-push-action@v6
        with:
          context: .
          file: ./frontend/Dockerfile
          push: true
          tags: ghcr.io/nguyenbinh19/mkt-tool-web:${{ github.sha }}
```

Chú ý: image backend cài Playwright Chromium nên build có thể lâu và tốn dung lượng; xem thời gian/chi phí runner thực tế trước khi thêm nhiều pipeline song song. Job `image-check` kiểm tra Dockerfile/Compose/Nginx trên PR; job publish vẫn build lại release image trong một run tin cậy sau merge, không sử dụng artifact từ PR chưa được tin cậy. Không chạy bài test đòi Facebook đăng nhập thật trong PR CI; tách nó thành test thủ công trên môi trường chuyên dụng.

## 9. Script deploy trên server

Tạo `/usr/local/sbin/leadflow-deploy` bằng editor trên server (không đưa `.env` vào Git). Mẫu dưới đây **chỉ dành cho deploy sau lần khởi tạo đầu**, khi MySQL container đã chạy:

```bash
#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 || ! $1 =~ ^[0-9a-f]{40}$ ]]; then
  echo 'Expected exactly one 40-character lowercase commit SHA.' >&2
  exit 2
fi

cd /opt/leadflow
umask 077
export IMAGE_TAG="$1"
old_tag="$(cat .deployed-sha 2>/dev/null || true)"
compose=(docker compose --env-file .env -f docker-compose.yml -f docker-compose.cd.yml)

mkdir -p /var/backups/leadflow
backup="/var/backups/leadflow/mysql-$(date -u +%Y%m%dT%H%M%SZ).sql"
"${compose[@]}" exec -T mysql sh -c 'exec mysqldump --single-transaction --quick -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > "$backup"
test -s "$backup"

"${compose[@]}" pull backend nginx
started=1
"${compose[@]}" up -d --no-build backend nginx || started=0

healthy=0
if [[ "$started" == 1 ]]; then
  for attempt in {1..30}; do
    web_status="$(curl -sS -o /dev/null -w '%{http_code}' -H 'Host: stayhubb2b.site' http://127.0.0.1/ || true)"
    api_status="$(curl -sS -o /dev/null -w '%{http_code}' -H 'Host: stayhubb2b.site' http://127.0.0.1/api/workspaces || true)"
    if [[ "$web_status" == 200 && "$api_status" == 401 ]]; then
      healthy=1
      break
    fi
    sleep 5
  done
fi

if [[ "$healthy" != 1 ]]; then
  echo 'Smoke test failed.' >&2
  if [[ "$old_tag" =~ ^[0-9a-f]{40}$ ]]; then
    export IMAGE_TAG="$old_tag"
    "${compose[@]}" pull backend nginx
    "${compose[@]}" up -d --no-build backend nginx
    echo "Previous image $old_tag requested. Check database schema compatibility." >&2
  fi
  exit 1
fi

printf '%s\n' "$IMAGE_TAG" > .deployed-sha
echo "Deployed $IMAGE_TAG; database backup: $backup"
```

**Trước khi dùng mẫu này:** kiểm tra `127.0.0.1:80` thực sự là Nginx của project; nếu `HTTP_PORT` khác 80 hoặc dùng Cloudflare Tunnel không publish port ra host, đổi smoke test sang kiểm tra nội bộ container. Nên chạy `shellcheck` và thử trên staging/VM trước production. Script rollback **chỉ quay image**, không quay schema MySQL. Nếu Hibernate đã thay đổi schema không tương thích với image cũ, phục hồi DB theo runbook riêng.

Đặt quyền:

```bash
sudo chown root:root /usr/local/sbin/leadflow-deploy /opt/leadflow/docker-compose.cd.yml
sudo chmod 755 /usr/local/sbin/leadflow-deploy
sudo chmod 644 /opt/leadflow/docker-compose.cd.yml
sudo chmod 600 /opt/leadflow/.env
```

Không `chown -R` cả `/opt/leadflow` vì trong đó có profile Facebook và dữ liệu khác. Kiểm tra user `deploy` không có quyền ghi vào thư mục cha `/opt/leadflow` hoặc các file Compose/script; nếu có, rule sudo một script sẽ không còn là ranh giới bảo mật thực sự.

## 10. Workflow deploy production

Tạo `.github/workflows/deploy-prod.yml`. Sau khi `ci.yml` trên `main` hoàn tất và cả hai image đã có trên GHCR, vào Actions → **Deploy production** → Run workflow trên `main`. Nếu Environment có reviewer, GitHub sẽ chờ phê duyệt. `workflow_dispatch` chỉ hiện khi workflow tồn tại trên default branch.

```yaml
name: Deploy production
on:
  workflow_dispatch:

permissions:
  contents: read
  id-token: write

concurrency:
  group: production-deploy
  cancel-in-progress: false

jobs:
  deploy:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: tailscale/github-action@v4
        with:
          oauth-client-id: ${{ secrets.TS_OAUTH_CLIENT_ID }}
          audience: ${{ secrets.TS_AUDIENCE }}
          tags: tag:ci
          ping: 100.109.134.68
      - name: Deploy only the tested commit
        run: tailscale ssh deploy@100.109.134.68 "sudo -n /usr/local/sbin/leadflow-deploy ${{ github.sha }}"
```

Nếu tài khoản GitHub của bạn không hỗ trợ Environment secrets cho repo private, đặt hai secret Tailscale ở repository scope và giữ deploy chỉ qua `workflow_dispatch`/nhánh `main`; đừng truyền chúng vào PR workflow. CI node `tag:ci` chỉ được SSH thành user `deploy` trên server `tag:prod`.

## 11. Chạy lần đầu và kiểm tra

1. Trên PR đầu tiên, thêm workflow CI, để CI chạy; sửa hết lỗi trước khi merge.
2. Merge `main`; vào GitHub Actions kiểm tra job `publish` xanh. Mở GHCR Packages xác nhận **hai image cùng SHA**.
3. Trên server, xác nhận đã login GHCR (nếu package private), `docker-compose.cd.yml` hợp lệ, backup/restore đã kiểm thử và user `deploy` không có quyền sửa script/Compose/`.env`.
4. Chạy deploy bằng nút workflow. Trong log phải thấy Tailscale ping thành công, backup tạo ra, pull đúng SHA, smoke test 200/401, rồi ghi `.deployed-sha`.
5. Trên server kiểm tra:

```bash
cd /opt/leadflow
cat .deployed-sha
docker compose --env-file .env -f docker-compose.yml -f docker-compose.cd.yml ps
docker compose --env-file .env -f docker-compose.yml -f docker-compose.cd.yml logs --tail=100 backend
curl -I https://stayhubb2b.site/
```

6. Trên UI kiểm tra đăng nhập, mở workspace, Content Monitor, extract job và trạng thái sau refresh. Kiểm tra riêng phiên Facebook qua SSH/noVNC nếu backend vừa đổi Chromium/Playwright.

## 12. Rollback, cập nhật schema và sự cố thường gặp

- **Frontend/backend lỗi sau deploy:** xem SHA cũ trong lịch sử `.deployed-sha`/GitHub deployment; chạy lại script với SHA cũ sau khi xác nhận schema vẫn tương thích. Giữ tối thiểu hai image gần nhất trên server.
- **Cập nhật schema lỗi:** không xóa volume. Xem log Hibernate khi backend khởi động, đối chiếu entity với schema MySQL và phục hồi từ backup đã thử restore nếu cần.
- **GHCR pull bị 401:** kiểm tra package visibility, PAT `read:packages` trên server và `docker login ghcr.io`. Không dùng token ghi package trên server.
- **Tailscale SSH đòi xác minh trình duyệt:** rule cho `tag:ci` chưa là `accept`, server chưa gắn `tag:prod`, hoặc grant TCP 22/SSH user `deploy` thiếu. Không tắt toàn bộ SSH security để né lỗi.
- **Nginx báo 502:** backend chưa healthy; xem `docker compose ps` và log backend. DB hoặc Hibernate có thể đang khởi động.
- **CI xanh nhưng Facebook không chạy:** CI không dùng account Facebook thật. Kiểm tra profile mount, noVNC, quyền file profile và checkpoint thủ công; không đưa Facebook credential vào pipeline.
- **Không đủ đĩa:** dọn image cũ có chọn lọc sau khi backup và ghi SHA rollback; không chạy lệnh prune volume.

## 13. Điều kiện gọi là hoàn tất

- [ ] PR không thể merge nếu backend/frontend CI đỏ.
- [ ] Merge `main` tạo đúng hai GHCR image cùng SHA; server không build source.
- [ ] Production deploy chỉ từ `main`, có thao tác phê duyệt phù hợp với gói GitHub.
- [ ] CI không có quyền root, không thuộc Docker group, không đọc `.env`/cookie Facebook.
- [ ] Backup MySQL trước deploy và có bài thử restore định kỳ, lưu ngoài server.
- [ ] Nginx/backend/MySQL healthy; smoke test không phụ thuộc mật khẩu admin bootstrap.
- [ ] Rollback image cũ đã được thử trên staging; schema sau cập nhật vẫn tương thích ngược.
- [ ] Đường Cloudflare → origin đã bảo vệ bằng Tunnel hoặc Full (strict), không dựa vào Flexible.

## Tài liệu gốc để tìm hiểu thêm

- [GitHub Actions: publish Docker images](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
- [GitHub Environments và giới hạn theo gói](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
- [GitHub manual workflow](https://docs.github.com/actions/managing-workflow-runs/manually-running-a-workflow)
- [GitHub GHCR authentication](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Tailscale GitHub Action](https://tailscale.com/docs/integrations/github/github-action)
- [Tailscale SSH access rules](https://tailscale.com/docs/features/tailscale-ssh)
- [Docker Compose startup/health](https://docs.docker.com/compose/how-tos/startup-order)
- [Docker volume backup/restore](https://docs.docker.com/engine/storage/volumes/)
