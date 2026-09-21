# Linux deployment

The production stack is defined in `docker-compose.yml`:

- `mysql` stores application data
- `backend` runs Spring Boot on the private Docker network
- `nginx` serves the existing `frontend/dist` folder and proxies `/api/*` to `backend:8080`

Build the frontend once before starting the containers:

```bash
cd frontend
npm ci
npm run build
cd ..
```

Then, from the project root:

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f --tail=100 backend
```

The site is then available at `http://SERVER_IP/`. The default seeded administrator is:

```text
admin@leadflow.local / admin123
```

Change the password after the first login. To update a deployment:

```bash
docker compose pull
docker compose up -d --build
docker image prune -f
```

The MySQL data is kept in the named volume `leadflow_mysql_data`. Do not run
`docker compose down -v` unless you intentionally want to delete the database.

For Facebook extraction on a server, `FACEBOOK_HEADLESS=true` is enabled by the
compose file. If Facebook requires a fresh login, stop the backend, copy a
Playwright profile into `./playwright-profile`, and start the stack again.
