# LeadFlow backend

The backend is a Spring Boot 4 / Java 21 application. It owns authentication, RBAC, MySQL persistence, and the Playwright integration used by the Facebook tools.

## API contract

Every application endpoint begins with `/api/`:

- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/auth/me`
- `/api/workspaces/**`
- `/api/facebook/**`
- `/api/access-requests/**`, `/api/admin/**`, and the private health probe `/api/actuator/health`

Except for login, registration, and the private health probe, clients must include `Authorization: Bearer <token>`. The health probe stays behind Nginx access controls and is not available from the public internet.

## Profiles

- `application-local.yaml` is for local development and permits a visible browser session.
- `application-prod.yaml` obtains database and bootstrap values exclusively from environment variables. Both profiles use Hibernate `ddl-auto: update` to create or add schema objects from the JPA entities on startup. Docker Compose runs Chromium on a private virtual display for manual Facebook sign-in over an SSH tunnel.

For a first production database, set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` before start. The configured admin is created once, approved, and stored with a BCrypt password hash. The backend refuses a bootstrap password shorter than 12 characters.

## Facebook session

The service only uses a Playwright profile owned by the deployment operator. Sign in manually where permitted, keep the mounted profile directory private, and do not put platform credentials in source code or environment variables.

Chromium is launched with a loopback-only CDP endpoint (default port 9222), then each request connects through that endpoint. Do not keep an older project Chromium window open when restarting a local backend: the old window locks the profile and may occupy the port. Close that window manually first; never delete the profile to fix a port conflict. `FACEBOOK_DEBUG_PORT` can select another unused port when 9222 belongs to an unrelated application.

Local development now defaults to `../playwright-profile-v2` because the original local `../playwright-profile` stopped loading even Facebook Home while a clean profile loaded the same group permalink successfully. The original profile is preserved. After restarting the backend, click **Open Facebook session** and sign in again. If `FACEBOOK_PROFILE_DIRECTORY` is set in IntelliJ or the environment, update or remove that override; production keeps its existing mounted profile and is not affected.

The browser itself runs on the server and starts preparing after the backend is ready. Each extraction or content-search request gets its own Playwright connection and a tab in the shared authenticated Chromium profile. `FACEBOOK_MAX_CONCURRENT_TABS` (default 1) controls both the fair tab semaphore and extraction worker count; additional extraction requests enter a bounded FIFO queue (100 waiting jobs maximum), so `POST /api/facebook/extract-jobs` returns `202 Accepted` immediately. `GET /api/facebook/extract-jobs?workspaceId=...&page=0` lists jobs in pages; `GET /api/facebook/extract-jobs/{id}` returns a result. Admins can inspect `GET /api/facebook/browser/status`, open the login tab with `POST /api/facebook/browser/open`, and restart the project browser with `POST /api/facebook/browser/retry` when no request tabs are active. The backend also retries a browser process that exits unexpectedly; it does not restart a healthy browser. Finished jobs can be cleared per workspace with `DELETE /api/facebook/extract-jobs?workspaceId=...`. Jobs and results are stored in MySQL and scoped to the owner.
