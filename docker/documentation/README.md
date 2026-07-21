<!--
  ~ Copyright 2011-2026 The OTP authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->

# Local monitoring documentation

The local monitoring profile starts Prometheus and Grafana alongside the OTP development environment.

## Start the stack

From the repository root, run:

```bash
docker compose --profile dev --profile monitoring up -d --build
```

The first start can take a few minutes while OTP is built and Keycloak imports the `otp-dev` realm.

## Running without monitoring

Monitoring is optional. To start the development environment on its own, drop the `monitoring` profile:

```bash
docker compose --profile dev up -d --build
```

Prometheus and Grafana are then not started, and OTP behaves exactly as before — the monitoring stack only adds the
metrics collection and dashboards on top.

The default endpoints are:

- OTP: <http://localhost:8080>
- Keycloak: <http://localhost:8100>
- Grafana: <http://localhost:3000>
- Prometheus: `http://prometheus:9090` inside the Docker network; it is not exposed on the host

## Check readiness

Check that Keycloak's OpenID configuration is available:

```bash
curl -sf http://localhost:8100/realms/otp-dev/.well-known/openid-configuration | jq -r .issuer
```

The expected issuer is `http://localhost:8100/realms/otp-dev`.

Check that Grafana responds with a redirect to its login flow:

```bash
curl -sf -o /dev/null -w '%{http_code}\n' http://localhost:3000/
```

The expected status is `302`. Check the Prometheus container state with:

```bash
docker compose ps prometheus
```

## Log in to Grafana

1. Open <http://localhost:3000>.
2. Select **Sign in with DKFZ SSO**.
3. Log in with username `grafana-admin` and password `grafana`.

Grafana provisions the **JVM (Micrometer)** dashboard automatically. Open the **OTP** folder under
<http://localhost:3000/dashboards> and select the dashboard. The `Application` and `Instance` variables resolve to
`otp` and `otp:8080` after OTP starts exposing metrics.

A few panels (Errors, Utilisation, JVM Process Memory) require OS-level metrics that the JVM exporter alone doesn't
provide and will remain "No data" — this is expected.

## Stop the stack

```bash
docker compose --profile dev --profile monitoring down
```
