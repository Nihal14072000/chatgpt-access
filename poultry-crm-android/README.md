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

The Android data layer contains Retrofit contracts for generic CRUD, finance and trip endpoints, a shared OkHttp client, Basic-auth sign-in through `/auth/me`, an in-memory session, and an HTTP 401 session-clear interceptor. The Compose login screen calls the existing authentication endpoint; passwords are not persisted.

**Important backend finding:** the web app's generic `BusinessDataService` is currently wired to `JsonFileAdapter` in `app.config.ts`, which serves local mock fixtures. Its `ApiDataAdapter` exists in source but is not the adapter selected by the current app configuration. Dedicated services such as finance and trips do call REST endpoints. The Android app is being built to use the real REST contracts rather than treating mock JSON fixtures as server APIs.

## Configured backend host

The Android app's default API base URL is:

```
http://129.225.90.181:1111/api/
```

This uses the supplied server host and the `/api/` prefix used by the web application's API URL convention. Override it at build time with `-PPOULTRY_API_BASE_URL=https://your-api-host.example/api/` if the backend address changes. The value must end with a slash.

The supplied endpoint uses plain HTTP. Cleartext traffic is currently enabled for development in the app manifest; use HTTPS and disable cleartext traffic before a production release. The host and API routes have not yet been connectivity-tested from an Android device or emulator.

## Open in Android Studio

Open this directory as a project. Use JDK 17 and Android SDK 35.

## Current implementation

The first UI milestone includes project setup, a dashboard, module navigation, a searchable module directory and representative sample records. The shared backend networking layer is scaffolded against the web app's observed endpoint contracts.

Login is wired to the shared authentication endpoint. The business module screens are still sample-data UI and need to be connected module-by-module to the corresponding live API endpoints. The backend host, API availability, response schemas, and any server-side differences must be confirmed before production use.

## Planned feature parity

- Farm operations: farms, sheds, flocks/batches, feed, health, medication and vaccination
- Inventory: products, warehouses, stock, transfers and stock ledger
- Trading: customers, suppliers, quotations, sales orders, dispatch and procurement
- CRM: leads, opportunities, follow-ups and complaints
- Logistics: employees, vehicles, assignments, trips and deliveries
- Finance: receivables, payables, payments, receipts, expenses and profitability
- Reports, analytics, notifications, organization, users and role permissions

## Automated APK build

GitHub Actions builds a debug APK on pushes to `feature/poultry-crm-android`. The APK is uploaded as the `poultry-crm-android-debug` workflow artifact.
