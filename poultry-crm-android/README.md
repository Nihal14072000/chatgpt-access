# Poultry CRM — Android

Native Android implementation workspace based on the feature set of the Poultry CRM `v5` web application.

This project is isolated in this directory. Existing repository content is not modified.

## Stack
- Kotlin
- Jetpack Compose and Material 3
- Navigation Compose
- Offline sample data for the first UI milestone

## Open in Android Studio
Open this directory as a project. Use JDK 17 and Android SDK 35.

## Current milestone
Android project setup, dashboard, module navigation, searchable module directory, and representative offline sample records.

This is an initial UI foundation, not yet a complete port of the web CRM. Backend APIs, authentication, Room persistence, synchronization, role enforcement, and full CRUD workflows remain to be implemented and validated.

## Planned feature parity
- Farm operations: farms, sheds, flocks/batches, feed, health, medication and vaccination
- Inventory: products, warehouses, stock, transfers and stock ledger
- Trading: customers, suppliers, quotations, sales orders, dispatch and procurement
- CRM: leads, opportunities, follow-ups and complaints
- Logistics: employees, vehicles, assignments, trips and deliveries
- Finance: receivables, payables, payments, receipts, expenses and profitability
- Reports, analytics, notifications, organization, users and role permissions
