# Deploy LeadFlow with Docker and Cloudflare

This deployment exposes one public web application. Nginx serves the React dashboard and proxies every application API request under `/api/` to the private Spring Boot container. MySQL and the backend port are never published to the internet.

## 1. Prepare the server

Install Docker Engine and Docker Compose plugin on an Ubuntu/Debian server. Allow SSH and HTTP (80) through the server firewall. Clone or copy this repository to a private directory on the server.

## 2. Create the production environment file

On a fresh server, run `bash deploy/provision-env.sh` from the project directory. It creates `.env` with unique random MySQL and admin passwords and leaves an existing `.env` untouched. Keep this file readable only by root.

```bash
bash deploy/provision-env.sh
grep '^BOOTSTRAP_ADMIN_' .env
```

`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` are required. On the first start with an empty database, the backend creates exactly one approved `ADMIN` account using these values. The password must have at least 12 characters and is BCrypt-hashed before it is stored. Later restarts never reset or overwrite that account.

Do not commit `.env`, share it in chat, or use `admin123` in production.

## 3. Start and verify

```bash
docker compose config
docker compose up -d --build
docker compose ps
curl -fsS http://127.0.0.1/healthz
docker compose logs --tail=100 backend
```

Open the site through its Cloudflare domain to sign in and create a workspace. Unauthenticated requests to `/api/workspaces` must receive an authorization error.

## 4. Connect `stayhubb2b.site` through Cloudflare

1. In Cloudflare DNS, create an `A` record for `stayhubb2b.site` pointing to the server IPv4 address. Optionally create `www` as a CNAME to the root domain.
   If the server is behind a home router, also forward TCP port 80 from the router to the server's LAN address. DNS alone cannot reach a machine behind NAT. A Cloudflare Tunnel is an alternative when port forwarding is unavailable.
2. Turn on the Cloudflare proxy (orange cloud) after the origin responds correctly.
3. Set Cloudflare SSL/TLS mode to **Flexible** when this Nginx origin listens only on port 80. Cloudflare then terminates HTTPS for visitors and forwards HTTP to the server.
4. Enable **Always Use HTTPS** in Cloudflare after the proxied domain is working.

The supplied Nginx configuration already recognises `stayhubb2b.site`, listens on port 80, and sends application requests only to `/api/`. No certificate files or port 443 configuration are required for the Cloudflare Flexible setup. If you later want encrypted Cloudflare-to-server traffic, change to Full (strict) and add an origin certificate separately.

## 5. Facebook browser profile

The backend runs Chromium on a private virtual display. `PLAYWRIGHT_PROFILE_DIRECTORY` points to a persistent private directory mounted into the backend container. To sign in manually:

1. On your Windows PC, run `ssh -L 6080:127.0.0.1:6080 root@100.109.134.68` and leave that CMD window open.
2. Open `http://127.0.0.1:6080/vnc.html` in your local browser, then click **Connect**.
3. In the LeadFlow UI, sign in as admin and click **Mở phiên Facebook**. The Chromium window appears in the remote display. Sign in yourself and complete any verification there.

Port 6080 is bound only to the server loopback interface, so the remote display is available through the SSH tunnel, not through the public website. Protect the profile directory like a password and never put Facebook credentials in `.env`.

The backend prepares the server browser and an admin login tab on startup. Set `FACEBOOK_MAX_CONCURRENT_TABS` in `.env` to control simultaneous request tabs (default 1). Extra extraction jobs wait in a bounded queue; the UI stays usable and shows job status. The queue accepts up to 100 waiting jobs; when full, the API returns HTTP 429. Jobs and results are stored in MySQL, and a container restart resumes queued or interrupted jobs. The admin console shows browser status and provides manual open and retry actions. The private remote display remains the place to sign in.

## Operations

```bash
# Rebuild after pulling source changes
docker compose up -d --build

# Follow a service log
docker compose logs -f --tail=100 backend

# Database backup (write the dump outside the repository)
docker compose exec -T mysql sh -c 'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > leadflow-backup.sql
```

On this server, run `bash deploy/smoke-test.sh` after each deploy. The MySQL volume is named `leadflow_leadflow_mysql_data` when the project directory is `/opt/leadflow`. Never run `docker compose down -v` unless intentionally destroying all application data and backups have been verified.

To check the configured concurrency limit, run `bash deploy/test-browser-concurrency.sh`. It submits one more job than the configured limit in a temporary workspace and checks that the excess job waits. Run it only when the Facebook session is ready; rapidly failing jobs can finish before the queue is observable.
