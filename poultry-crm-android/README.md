# Poultry CRM — Android

Native Android implementation workspace based on the feature set of the Poultry CRM `v5` web application.

This project is isolated in this directory. Existing repository content is not modified.

## Stack
- Kotlin
- Jetpack Compose and Material 3
- Navigation Compose
- Retrofit + OkHttp for the existing REST backend

## Existing web backend compatibility

The Android client is being aligned with the API contracts found in the web app's `v5` source:

- API base URL convention: `{apiUrl}`
- Authentication: `GET /auth/me` with HTTP Basic authorization
- Generic CRUD adapter convention: `/v1/{resource}`, `POST /v1/{resource}`, `PUT /v1/{resource}/{id}`, and `DELETE /v1/{resource}/{id}`
- Finance endpoints used by the web app: `/payments/retrieve`, `/payments/{id}/retrieve`, `/payments`, `/finance/settlements`, `/finance/receivables`, `/finance/payables`, and `/finance/allocations`
- Trip support endpoints used by the web app: `/trips`, `/vehicles`, `/drivers`, `/supervisors`, `/buying-parties`, `/selling-parties`, and `/order-types`

The Android data layer now contains Retrofit contracts, a shared OkHttp client, Basic-auth session handling, and an HTTP 401 session-clear interceptor.

**Important backend finding:** the web app's generic `BusinessDataService` is currently wired to `JsonFileAdapter` in `app.config.ts`, which serves local mock fixtures. Its `ApiDataAdapter` exists in source but is not the adapter selected by the current app configuration. Dedicated services such as finance and trips do call REST endpoints. The Android app is being built to use the real REST contracts rather than treating mock JSON fixtures as server APIs.

## Configure the backend host

The web source currently sets `apiUrl` to `http://localhost:8080/api`. Android Emulator reaches the development machine's localhost using `10.0.2.2`, so the Android default is:

```
http://10.0.2.2:8080/api/
```

For a physical device, set `POULTRY_API_BASE_URL` to the backend machine's reachable LAN or deployed HTTPS URL, ending in `/api/`. Example Gradle invocation:

```bash
./gradlew :app:assembleDebug -PPOULTRY_API_BASE_URL=https://your-api-host.example/api/
```

Do not ship HTTP cleartext traffic in production. Use HTTPS and disable cleartext traffic for release builds.

## Open in Android Studio

Open this directory as a project. Use JDK 17 and Android SDK 35.

## Current implementation

The first UI milestone includes project setup, a dashboard, module navigation, a searchable module directory and representative sample records. The shared backend networking layer is now scaffolded against the web app's observed endpoint contracts.

The login and business screens still need to be wired end-to-end to the live backend and tested against a reachable backend environment. Backend host, actual API availability, response schemas, and any server-side differences must be confirmed before production use.

## Planned feature parity

- Farm operations: farms, sheds, flocks/batches, feed, health, medication and vaccination
- Inventory: products, warehouses, stock, transfers and stock ledger
- Trading: customers, suppliers, quotations, sales orders, dispatch and procurement
- CRM: leads, opportunities, follow-ups and complaints
- Logistics: employees, vehicles, assignments, trips and deliveries
- Finance: receivables, payables, payments, receipts, expenses and profitability
- Reports, analytics, notifications, organization, users and role permissions
