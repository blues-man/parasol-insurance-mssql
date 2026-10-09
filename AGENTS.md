# AGENTS.md — Guidance for AI agents working on this repo

Parasol Insurance demo: a Quarkus 3.40.1 (Java 21) REST application backed by
**SQL Server 2022**, deployed to a **Red Hat OpenShift** cluster (namespace
`nvinto-dev`) via the Helm chart in `helm/parasol/`.

## Project layout

```
pom.xml                          Maven build; quarkus-jdbc-mssql is the DB driver
src/main/resources/application.properties   Datasource config (MSSQL_* env vars)
src/main/resources/import.sql    Seed data (executed by Hibernate drop-and-create)
Containerfile                    UBI9 openjdk-21-runtime image; copies target/quarkus-app/
helm/parasol/                    Helm chart: app + SQL Server
k8s/mssql/                       Raw MSSQL manifests (Microsoft quickstart, OpenShift tab)
```

## Database

- SQL Server 2022 container image `mcr.microsoft.com/mssql/server:2022-latest`,
  Developer edition, data on an 8Gi RWO PVC.
- Datasource env vars: `MSSQL_HOST`, `MSSQL_USER` (default `sa`),
  `MSSQL_PASSWORD`, `MSSQL_DATABASE` (default `parasol`).
- The JDBC URL includes `encrypt=false;trustServerCertificate=true` — required
  because the container has no trusted TLS certificate.
- SQL Server does **not** auto-create the `parasol` database. The Helm chart's
  app pod includes an `mssql-db-create` init container (uses the sqlcmd bundled
  in the SQL Server image) that waits for SQL Server and creates it. If you
  deploy with the raw manifests in `k8s/mssql/` instead, create it manually:
  ```
  oc exec -n nvinto-dev deploy/mssql-deployment -- /opt/mssql-tools18/bin/sqlcmd \
    -S localhost -U sa -P '<password>' -C -Q "IF DB_ID('parasol') IS NULL CREATE DATABASE parasol"
  ```
- Schema is managed by Hibernate `quarkus.hibernate-orm.schema-management.strategy=drop-and-create`
  with `import.sql` seed data. `ALTER SEQUENCE claim_seq RESTART WITH 9;` is valid
  T-SQL — Panache's `@GeneratedValue` resolves to the `claim_seq` sequence on SQL Server.

## Build

```
./mvnw package -DskipTests
```

Produces `target/quarkus-app/` (fast-jar), which the Containerfile copies.

## Containerize — IMPORTANT: platform flag

The typical host is Apple Silicon, but the OpenShift nodes are **x86_64**.
Podman defaults to the host architecture, producing a `linux/arm64` image that
fails on the cluster with `exec format error`. **Always build with:**

```
podman build --platform linux/amd64 -t quay.io/bluesman/parasol-mssql:latest -f Containerfile .
podman push quay.io/bluesman/parasol-mssql:latest
```

(No in-image compilation happens, so no emulation is needed.)

## Deploy

```
helm upgrade --install parasol helm/parasol -n nvinto-dev
```

Gotchas learned the hard way:

- **Do NOT use `--create-namespace`** — the user lacks permission to patch
  namespaces; `nvinto-dev` already exists.
- **No LoadBalancer services** — the `for-nvinto-services` ResourceQuota in
  `nvinto-dev` sets `services.loadbalancers: 0`. Keep
  `mssql.serviceType=ClusterIP` (the default).
- When the tag is `latest`, `helm upgrade` alone won't re-pull the image — run
  `oc rollout restart deployment/parasol-app -n nvinto-dev` after pushing.
- If the repo on quay.io is private, create a pull secret:
  `oc create secret docker-registry parasol-quay-pull -n nvinto-dev --from-file=.dockerconfigjson=$HOME/.config/containers/auth.json`
  and set it under `image.pullSecrets` in `helm/parasol/values.yaml`.

## Verification

```
oc get pods -n nvinto-dev -l 'app in (parasol-app, parasol-mssql)'
curl -s https://parasol-app-nvinto-dev.apps.rm3.7wse.p1.openshiftapps.com/api/claims
```

The app exposes health probes at `/q/health/ready` and `/q/health/live`.
The OpenShift Route is `parasol-app-nvinto-dev.apps.rm3.7wse.p1.openshiftapps.com`.

## Application notes

- Kafka was **removed**: `EmailGenerator` (scheduled every 45s) classifies and
  stores demo emails in-process via `EmailRouter`/`EmailStore`. Don't
  reintroduce `quarkus-messaging-kafka` or reactive-messaging config.
- Dev DB credentials (public demo): `sa` / `P@rasol-2026-Sql`. Override in
  deployments with `--set mssql.saPassword=<strong-password>`.
