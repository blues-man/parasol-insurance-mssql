# Parasol Helm Chart

Deploys the Parasol Insurance Quarkus app (`quay.io/bluesman/parasol-mssql`) together with
SQL Server 2022 (per the [Microsoft quickstart for OpenShift](https://learn.microsoft.com/en-us/sql/linux/install-upgrade/quickstart-containers-azure?view=sql-server-ver16&tabs=oc))
into a single namespace.

## Resources created

| Resource | Template |
| --- | --- |
| Secret (SA password) | `mssql-secret.yaml` |
| PVC `mssql-data` (8Gi) | `mssql-pvc.yaml` |
| Deployment `mssql` (SQL Server 2022) | `mssql-deployment.yaml` |
| Service `mssql` (1433) | `mssql-service.yaml` |
| Deployment `parasol` (Quarkus app) | `app-deployment.yaml` |
| Service `parasol` (8080) | `app-service.yaml` |
| Route (OpenShift, edge TLS) | `app-route.yaml` |

The app pod has an **init container** that waits for SQL Server to accept connections and
creates the `parasol` database (`SQL Server` does not create it automatically), so the app's
Hibernate `drop-and-create` schema generation works on first boot.

## Install

```console
helm install parasol helm/parasol -n nvinto-dev --create-namespace
```

Override the SA password (recommended):

```console
helm install parasol helm/parasol -n nvinto-dev \
  --set mssql.saPassword='<your-strong-password>'
```

The app is exposed on the Route host:

```console
oc get route parasol-app -n nvinto-dev -o jsonpath='{.spec.host}'
```

## Notes

- `mssql.serviceType` defaults to `ClusterIP` (in-namespace only; the OpenShift quota in
  `nvinto-dev` forbids LoadBalancer services). The Microsoft guide uses `LoadBalancer` —
  set it back with `--set mssql.serviceType=LoadBalancer`.
- To reuse an existing secret instead of creating one, set `mssql.existingSecret=<name>`
  (it must contain the `MSSQL_SA_PASSWORD` key).
