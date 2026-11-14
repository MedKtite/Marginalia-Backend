# Deploying the Readsake backend

```
push to prod ─▶ GitHub Actions: test (mvn verify) ─▶ build image ─▶ ghcr.io/medktite/readsake-backend:<sha>
                                                                     │
                                       ssh medktite@ancora ◀─────────┘
                                       /opt/readsake: docker compose pull && up -d
```

Server: the Ancora VPS (`ssh ancora`, 2.24.59.205). Readsake has its own compose
project in `/opt/readsake` (db, backend). HTTPS comes from the Caddy that
already runs in `/opt/ancora`; the backend joins its network (`ancora_default`)
so Caddy can reach it as `readsake-backend`. On that network Ancora's Postgres
answers to `db`, so the backend reaches its own database as `readsake-db`.

Book files are **not** on the VPS: they live in Cloudflare R2 (bucket
`readsake-books`); phones download them with presigned URLs and keep a local
copy. The VPS only holds the database.

| URL | Goes to |
|---|---|
| https://api.readsake.io | backend :8080 |
| 127.0.0.1:5441 on the VPS | Postgres (SSH tunnel only) |

## One-time setup

1. **DNS** (Cloudflare): `A api → 2.24.59.205`, **DNS only (grey cloud)** so Caddy
   can get its own certificate.
1b. **R2** (Cloudflare → R2): create bucket `readsake-books`, then an API token with
   *Object Read & Write* on that bucket only. Its keys and endpoint go in `.env`.
2. **Folder**: `ssh -t ancora 'sudo install -d -o medktite -g medktite /opt/readsake /opt/readsake/secrets'`
3. **Secrets**: copy `deploy/.env.example` to `/opt/readsake/.env`, fill it, `chmod 600`.
4. **Caddy**: append to `/opt/ancora/Caddyfile`, then
   `docker exec ancora-caddy-1 caddy reload --config /etc/caddy/Caddyfile`:
   ```
   api.readsake.io {
   	reverse_proxy readsake-backend:8080
   }
   ```
5. **GitHub secrets** (repo → Settings → Secrets → Actions, environment `production`):
   `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` (private key whose public half is in the
   VPS `~/.ssh/authorized_keys`), `DEPLOY_KNOWN_HOSTS` (`ssh-keyscan 2.24.59.205`).
6. **First deploy**: push to `prod` (or Actions → *Build & deploy backend* → Run workflow on `prod`).
7. **Schema**, from the umbrella repo root once the db container is up:
   ```bash
   ssh ancora 'cd /opt/readsake && docker compose exec -T db psql -U readsake -d readsake' < marginalia-db/schema.sql
   for f in marginalia-db/migrations/*.sql; do
     ssh ancora 'cd /opt/readsake && docker compose exec -T db psql -U readsake -d readsake -v ON_ERROR_STOP=1' < "$f"
   done
   ```
   New migrations later: run only the new file the same way.

## Day to day

- Deploy = merge `main` into `prod` and push. Pushes to `main` and PRs run tests only.
- Logs: `ssh ancora 'docker logs -f readsake-backend-1'`
- Roll back: `ssh ancora 'cd /opt/readsake && BACKEND_TAG=<older sha> docker compose up -d backend'`
- DB from the Mac: `ssh -L 5441:127.0.0.1:5441 ancora`, then connect to `localhost:5441`.
