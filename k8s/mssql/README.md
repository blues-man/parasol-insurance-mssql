# SQL Server 2022 on OpenShift

Kubernetes/OpenShift manifests for deploying SQL Server 2022 into the `nvinto-dev`
namespace, following the Microsoft quickstart
["Deploy a SQL Server Container Cluster on AKS or Red Hat OpenShift"](https://learn.microsoft.com/en-us/sql/linux/tutorial-sql-server-containers-kubernetes)
(OpenShift tab). No namespace is hardcoded in the manifests — pass it with `-n nvinto-dev`.

## Files

| File | Purpose |
| --- | --- |
| `secret.yaml` | Generic Secret `mssql` holding `MSSQL_SA_PASSWORD` (the SA password used by the container and the app config). |
| `pvc.yaml` | PersistentVolumeClaim `mssql-data` (8Gi, ReadWriteOnce). Uses the cluster default storage class. |
| `sqldeployment.yaml` | Deployment `mssql-deployment` running `mcr.microsoft.com/mssql/server:2022-latest`, mounting the PVC at `/var/opt/mssql`. |
| `sqlservice.yaml` | Service `mssql-deployment` exposing port 1433. In-cluster in-namespace DB connectivity. |

## Apply order

```bash
oc apply -n nvinto-dev -f secret.yaml
oc apply -n nvinto-dev -f pvc.yaml
oc apply -n nvinto-dev -f sqldeployment.yaml
oc apply -n nvinto-dev -f sqlservice.yaml
```

(Or a single command: `oc apply -n nvinto-dev -f k8s/mssql/` — files are independent.)

## Verify

```bash
oc get pods -n nvinto-dev
```

Wait for `mssql-deployment-...` to reach `Running` and `1/1` ready. You can also check
`oc get pvc -n nvinto-dev` to confirm `mssql-data` is `Bound`.

## Create the `parasol` database

SQL Server does **not** auto-create databases, so after the pod is ready run:

```bash
oc exec -n nvinto-dev deploy/mssql-deployment -- /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<password>' -C -Q "IF DB_ID('parasol') IS NULL CREATE DATABASE parasol"
```

Replace `<password>` with the value from `secret.yaml` (or
`oc get secret mssql -n nvinto-dev -o jsonpath='{.data.MSSQL_SA_PASSWORD}' | base64 -d`).
The `-C` flag trusts the server's self-signed certificate, which `sqlcmd` v18 requires.

## Service type note

The Microsoft guide uses `type: LoadBalancer` to expose SQL Server outside the cluster.
Since the app that connects to this database runs in the same namespace, these manifests
use `type: ClusterIP` instead. If you ever need external access, change `type: ClusterIP`
to `type: LoadBalancer` in `sqlservice.yaml` (on OpenShift this provisions a cloud load
balancer via the LoadBalancer service capability; a Route may also be created).
