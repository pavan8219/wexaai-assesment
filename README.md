# FinTrace — Graph-Based Financial Fraud Investigation Platform

> **WEXA AI — CognoDB Take-Home Assignment**

FinTrace is a graph-powered financial fraud investigation platform built to identify suspicious transaction networks, shared account signals, and relationships between bank accounts, devices, IP addresses, and merchants.

The application uses **CognoDB Cloud** as the graph database and exposes the investigation capabilities through a Spring Boot REST API and an Angular web application.

**Live Demo:** https://fintrace-wexa.netlify.app/investigation  

---

## 1. What problem does FinTrace solve?

Financial transactions are not isolated events.

A suspicious payment may only become meaningful when we look at the **network behind it**:

```text
Account A001
    |
    | transfers money
    v
Account A002
    |
    | transfers money
    v
Account A003
    |
    | transfers money
    v
Account A004
    |
    | pays
    v
LuckyPlay Gaming
```

There can also be hidden relationships:

```text
A001 ──USES_DEVICE──> D001 <──USES_DEVICE── A002

A001 ──USES_IP──────> 103.21.45.10 <────USES_IP── A002
```

Looking at these connections as rows in separate relational tables requires multiple joins and repeated traversal logic.

FinTrace models these relationships directly as a graph so investigators can answer questions such as:

- Which accounts form a suspicious transaction chain?
- How many hops does money travel before reaching a high-risk merchant?
- Which accounts share the same device or IP address?
- Which accounts are connected through common signals?
- What does the complete relationship network around an account look like?
- Which merchants are connected to suspicious transaction networks?

---

# 2. Why a graph database?

This assignment specifically asks for a use case where a graph database genuinely earns its place.

Fraud investigation is a natural graph problem because the important information is **about relationships**, not just individual records.

## Relational approach

A relational implementation could require tables such as:

```text
accounts
transactions
merchants
devices
ip_addresses
account_devices
account_ips
```

Finding a multi-hop fraud chain would then require several joins.

For example:

```text
A001 -> A002 -> A003 -> A004 -> M003
```

would require repeatedly joining the transaction table.

## Graph approach

FinTrace represents the same information directly:

```text
(:BankAccount)-[:TRANSFERRED_TO]->(:BankAccount)
(:BankAccount)-[:PAYMENT_TO]->(:Merchant)
(:BankAccount)-[:USED_DEVICE]->(:Device)
(:BankAccount)-[:USED_IP]->(:IPAddress)
```

This makes relationship traversal a first-class database operation.

For example, a multi-hop investigation can be expressed naturally using Cypher:

```cypher
MATCH path =
  (start:BankAccount {accountId: $accountId})
  -[:TRANSFERRED_TO|PAYMENT_TO*2..4]->
  (merchant:Merchant)
WHERE merchant.riskLevel = 'HIGH'
RETURN path
```

The key advantage is that the query follows the graph instead of manually reconstructing the graph through multiple relational joins.

**In short: FinTrace uses CognoDB because the core question is "how are these entities connected?"**

This directly aligns with the assignment requirement to demonstrate a problem where connections and relationships are more important than rows in a table. fileciteturn1file0L40-L45

---

# 3. Application overview

FinTrace has three main layers:

```text
                    ┌─────────────────────────────┐
                    │        Angular UI           │
                    │                             │
                    │ Investigation Dashboard     │
                    │ Network Exploration         │
                    │ Shared Signals               │
                    │ Graph Visualization         │
                    └──────────────┬──────────────┘
                                   │ HTTP/JSON
                                   ▼
                    ┌─────────────────────────────┐
                    │      Spring Boot API        │
                    │                             │
                    │ Controllers                 │
                    │ Services                    │
                    │ Repositories                │
                    │ DTOs                        │
                    │ Error Handling              │
                    └──────────────┬──────────────┘
                                   │
                          Official Neo4j Driver
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │       CognoDB Cloud         │
                    │       Graph Database        │
                    │                             │
                    │ Nodes + Relationships       │
                    │ Cypher Queries              │
                    └─────────────────────────────┘
```

The application is deployed as:

```text
Angular
  → Netlify

Spring Boot
  → Render

Graph Database
  → CognoDB Cloud
```

---

# 4. Graph data model

FinTrace uses the following main node types:

| Node | Purpose | Important properties |
|---|---|---|
| `BankAccount` | Represents a bank account | `accountId` |
| `Merchant` | Represents a merchant | `merchantId`, `name`, `riskLevel` |
| `Device` | Represents a device used by an account | `deviceId` |
| `IPAddress` | Represents an IP address used by an account | `address` |

## Relationships

| Relationship | From | To | Properties |
|---|---|---|---|
| `TRANSFERRED_TO` | `BankAccount` | `BankAccount` | `amount` |
| `PAYMENT_TO` | `BankAccount` | `Merchant` | `amount` |
| `USED_DEVICE` | `BankAccount` | `Device` | — |
| `USED_IP` | `BankAccount` | `IPAddress` | — |

## Simple graph diagram

```text
                          ┌───────────────┐
                          │   Merchant    │
                          │ LuckyPlay     │
                          │   HIGH RISK   │
                          └───────▲───────┘
                                  │
                              PAYMENT_TO
                                  │
┌──────────┐   TRANSFERRED_TO  ┌──┴───────┐
│  A001    │ ────────────────> │   A002   │
└────┬─────┘                   └────┬─────┘
     │                              │
     │ USED_DEVICE                  │ TRANSFERRED_TO
     ▼                              ▼
┌──────────┐                    ┌──────────┐
│   D001   │                    │   A003   │
└──────────┘                    └────┬─────┘
                                     │
                                     │ TRANSFERRED_TO
                                     ▼
                                ┌──────────┐
                                │   A004   │
                                └────┬─────┘
                                     │
                                     │ PAYMENT_TO
                                     ▼
                              LuckyPlay Gaming

A001 ──USED_IP──> 103.21.45.10 <──USED_IP── A002
```

This model makes shared infrastructure and money movement directly traversable.

---

# 5. Example fraud network

One seeded suspicious network is:

```text
A001
 ↓ ₹80,000
A002
 ↓ ₹75,000
A003
 ↓ ₹70,000
A004
 ↓ ₹65,000
LuckyPlay Gaming
```

The same network can also contain shared signals:

```text
A001 ── D001 ── A002
A001 ── IP: 103.21.45.10 ── A002
```

This provides two independent relationship types:

1. **Financial relationship** — money moves between accounts.
2. **Identity/infrastructure relationship** — accounts share a device or IP.

Together these relationships provide stronger evidence for an investigation than either relationship alone.

---

# 6. Main investigation capabilities

## 6.1 Suspicious transaction networks

The application finds multi-hop paths that eventually reach high-risk merchants.

Example:

```text
A001 → A002 → A003 → A004 → M003
```

The API returns:

```json
{
  "sourceAccount": "A001",
  "network": ["A001", "A002", "A003", "A004", "M003"],
  "merchant": "LuckyPlay Gaming",
  "riskLevel": "HIGH",
  "hops": 4
}
```

The same analysis also identifies shorter suspicious paths such as:

```text
A001 → A002 → A003 → M003
A002 → A003 → A004 → M003
```

---

## 6.2 Shared signals

The application identifies accounts that share the same device or IP address.

Example:

```json
{
  "account1": "A001",
  "account2": "A002",
  "signalType": "Device",
  "sharedSignal": "D001"
}
```

and:

```json
{
  "account1": "A001",
  "account2": "A002",
  "signalType": "IPAddress",
  "sharedSignal": "103.21.45.10"
}
```

This allows an investigator to see that two accounts are connected even when there is no direct financial transfer between them.

---

## 6.3 Account network exploration

Given an account such as:

```text
A001
```

the application can return its connected graph:

```text
A001
 ├── A002
 │    └── A003
 │         └── A004
 │              └── A006
 │
 ├── A005
 │    └── A010
 │
 ├── Amazon
 ├── Device D001
 └── IP 103.21.45.10
```

The UI renders this graph visually so a non-technical user can explore relationships.

---

# 7. Main Cypher concepts

The application uses **parameterized Cypher queries** through the official Neo4j Java driver.

No user input is concatenated directly into Cypher.

For example:

```cypher
MATCH (a:BankAccount {accountId: $accountId})
RETURN a
```

with parameters:

```text
accountId = "A001"
```

This follows the assignment requirement for parameterized queries and avoids string-concatenated Cypher. fileciteturn1file0L46-L53

## Multi-hop traversal

The suspicious network query traverses multiple relationships:

```cypher
MATCH path =
  (start:BankAccount {accountId: $accountId})
  -[:TRANSFERRED_TO|PAYMENT_TO*2..4]->
  (merchant:Merchant)
WHERE merchant.riskLevel = 'HIGH'
RETURN path
```

This is an example of the type of graph traversal that is central to the application.

---

# 8. Why this query is awkward in a relational database

Suppose the investigation is:

> "Find accounts connected through multiple transfers that eventually pay a HIGH-risk merchant."

In a relational database, the application would need to repeatedly join transactions to themselves:

```text
transactions
    JOIN transactions
        JOIN transactions
            JOIN merchants
```

The number of joins grows as the number of hops grows.

In the graph model, the relationship itself is traversable:

```text
A001
 ↓
A002
 ↓
A003
 ↓
A004
 ↓
Merchant
```

This is one of the strongest reasons CognoDB is appropriate for FinTrace.

---

# 9. Seed data

The repository contains application seed logic that creates realistic fraud-investigation data.

The data includes:

- Bank accounts
- Merchants
- Devices
- IP addresses
- Account-to-account transfers
- Account-to-merchant payments
- Shared devices
- Shared IP addresses
- Merchant risk levels

Example accounts:

```text
A001
A002
A003
A004
A005
A006
A007
A008
A009
A010
```

Example merchants:

```text
Amazon
QuickMart
LuckyPlay Gaming
BetZone
CryptoFast
```

The seeded data deliberately contains both normal and suspicious relationships so the application can demonstrate meaningful graph queries.

This satisfies the assignment requirement for realistic seed data loaded through repository code. fileciteturn1file0L46-L52

---

# 10. Project structure

```text
fintrace-wexa/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/wexa/fintrace/
│   │   │   │
│   │   │   │   ├── config/
│   │   │   │   │   ├── CognoDBConfig.java
│   │   │   │   │   ├── DatabaseInitializer.java
│   │   │   │   │   └── SeedDataInitializer.java
│   │   │   │   │
│   │   │   │   ├── controller/
│   │   │   │   │
│   │   │   │   ├── dto/
│   │   │   │   │
│   │   │   │   ├── repository/
│   │   │   │   │
│   │   │   │   └── service/
│   │   │   │
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   │
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   └── ...
│
├── frontend/
│   ├── src/
│   ├── public/
│   │   └── _redirects
│   ├── package.json
│   └── angular.json
│
└── README.md
```

The backend follows a layered structure:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
CognoDB
```

DTOs are used to expose API-specific responses instead of exposing database implementation details directly.

---

# 11. Technology stack

## Backend

- Java
- Spring Boot
- Official Neo4j Java Driver
- openCypher
- Maven
- Docker

## Frontend

- Angular
- TypeScript
- HTML/CSS
- Graph visualization

## Database

- CognoDB Cloud
- openCypher
- Bolt protocol

CognoDB provides the managed graph database layer and is compatible with the official Neo4j drivers. This follows the technology guidance in the assignment. fileciteturn1file0L8-L13

---

# 12. Running locally

## Prerequisites

Install:

- Java 21
- Maven
- Node.js and npm
- Angular CLI
- A CognoDB Cloud account

The assignment provides a free CognoDB instance for development. fileciteturn1file0L20-L35

---

## 12.1 Create CognoDB instance

1. Create a CognoDB Cloud account.
2. Create a free `c0` instance.
3. Copy the connection URI.
4. Copy the generated password for the `cognodb` user.
5. Keep the password secure.

CognoDB's assignment instructions specify using the Bolt URI with the `cognodb` username and the generated password. fileciteturn1file0L20-L31

---

## 12.2 Configure backend

Create environment variables:

```text
COGNODB_URI=bolt+s://<your-instance>
COGNODB_USERNAME=cognodb
COGNODB_PASSWORD=<your-password>
```

The repository intentionally does **not** contain real database credentials.

`application.properties` expects:

```properties
spring.application.name=fintrace

cognodb.uri=${COGNODB_URI}
cognodb.username=${COGNODB_USERNAME}
cognodb.password=${COGNODB_PASSWORD}

server.port=${PORT:8080}
```

> Never commit your real CognoDB password to Git.

This follows the assignment requirement that connection details and passwords be read from environment variables and not committed to the repository. fileciteturn1file0L59-L62

---

## 12.3 Run backend

```bash
cd backend
```

Then:

```bash
./mvnw spring-boot:run
```

On Windows:

```cmd
mvnw.cmd spring-boot:run
```

The backend runs locally on:

```text
http://localhost:8080
```

---

## 12.4 Run frontend

Open another terminal:

```bash
cd frontend
npm install
npm start
```

The Angular application will be available on the local Angular development URL shown by the CLI.

The local frontend environment points to:

```text
http://localhost:8080
```

---

# 13. API overview

## Health

```http
GET /api/health/graph
```

Checks the application's connection to CognoDB.

---

## Suspicious networks

```http
GET /api/investigations/...
```

Returns suspicious multi-hop networks leading to high-risk merchants.

Example result:

```json
{
  "sourceAccount": "A001",
  "network": [
    "A001",
    "A002",
    "A003",
    "A004",
    "M003"
  ],
  "merchant": "LuckyPlay Gaming",
  "riskLevel": "HIGH",
  "hops": 4
}
```

---

## Shared signals

```http
GET /api/accounts/shared-signals
```

Returns accounts that share devices or IP addresses.

---

## Account graph

The application provides an account-level graph endpoint used by the UI to visualize:

- Accounts
- Merchants
- Devices
- IP addresses
- Transfers
- Payments
- Shared infrastructure

---

# 14. Error handling

The application handles database connectivity through a dedicated health check and service/repository boundaries.

If CognoDB becomes unreachable, the application can detect the database connection failure instead of silently returning misleading investigation results.

The health endpoint is:

```http
GET /api/health/graph
```

This is also used as a deployment health check.

Graceful database error handling is part of the engineering requirements for the assignment. fileciteturn1file0L59-L62

---

# 15. Security and configuration

Secrets are not hard-coded into the deployed application.

The repository contains:

```properties
cognodb.uri=${COGNODB_URI}
cognodb.username=${COGNODB_USERNAME}
cognodb.password=${COGNODB_PASSWORD}
```

Production environment variables are configured in the hosting platform.

### Backend

Hosted on Render:

```text
COGNODB_URI
COGNODB_USERNAME
COGNODB_PASSWORD
```

### Frontend

The production Angular environment contains only the public backend URL:

```text
https://fintrace-backend-g1tu.onrender.com
```

No database password is exposed to the frontend.

---

# 16. Deployment architecture

```text
                       USERS
                         │
                         ▼
              ┌─────────────────────┐
              │      Netlify        │
              │                     │
              │    Angular App      │
              └──────────┬──────────┘
                         │
                         │ HTTPS
                         ▼
              ┌─────────────────────┐
              │       Render        │
              │                     │
              │   Spring Boot API   │
              │     Dockerized      │
              └──────────┬──────────┘
                         │
                         │ Bolt
                         ▼
              ┌─────────────────────┐
              │     CognoDB Cloud   │
              │                     │
              │    Graph Database   │
              └─────────────────────┘
```

### Live application

https://fintrace-wexa.netlify.app/investigation

### Backend

https://fintrace-backend-g1tu.onrender.com

---

# 17. UI

The application is designed for a non-technical investigator.

The main workflow is:

```text
Open FinTrace
      ↓
Choose an investigation
      ↓
Select an account
      ↓
Explore suspicious networks
      ↓
Inspect shared signals
      ↓
Visualize the relationship graph
      ↓
Understand why the network is suspicious
```

The graph visualization makes the relationship structure visible instead of forcing an investigator to interpret raw database records.

## Screenshots

Add the final screenshots here before submission:

### Investigation dashboard

```text
docs/screenshots/investigation-dashboard.png
```

### Suspicious network results

```text
docs/screenshots/suspicious-networks.png
```

### Shared signals

```text
docs/screenshots/shared-signals.png
```

### Account graph

```text
docs/screenshots/account-graph.png
```

---

# 18. Example investigation

Consider account `A001`.

The graph reveals:

```text
A001
 │
 ├── TRANSFERRED_TO → A002
 │                      │
 │                      └── TRANSFERRED_TO → A003
 │                                             │
 │                                             └── TRANSFERRED_TO → A004
 │                                                                    │
 │                                                                    └── PAYMENT_TO → LuckyPlay Gaming
 │
 ├── USED_DEVICE → D001
 │                    ▲
 │                    │
 │                  A002
 │
 └── USED_IP → 103.21.45.10
                    ▲
                    │
                  A002
```

This is interesting because the application finds both:

```text
Financial connection
A001 → A002 → A003 → A004 → Merchant

Shared infrastructure
A001 ←→ Device D001 ←→ A002
A001 ←→ IP 103.21.45.10 ←→ A002
```

An investigator can therefore move from a single account to the broader connected network.

---

# 19. Design decisions

## Why CognoDB?

Because the application's primary operations are graph traversals.

## Why Spring Boot?

It provides a clean layered architecture for REST APIs, configuration, exception handling, and integration with the official Neo4j Java driver.

## Why Angular?

The investigation workflow benefits from an interactive UI and graph visualization.

## Why Docker?

The backend can be built consistently in the deployment environment without committing compiled JAR files.

## Why environment variables?

Database credentials are deployment secrets and must not be committed to source control.

---

# 20. Assignment requirement mapping

The following table maps FinTrace to the requirements in the WEXA AI assignment.

| Assignment requirement | FinTrace implementation |
|---|---|
| CognoDB as graph database | CognoDB Cloud |
| Thoughtful graph model | Accounts, merchants, devices, IPs + typed relationships |
| Node/relationship properties | Account IDs, merchant risk, transaction amounts, etc. |
| Simple model diagram | Included above |
| Realistic seed data | SeedDataInitializer |
| Seed script/code in repository | Backend seed initialization |
| Multi-hop Cypher | Suspicious network traversal |
| Relationally awkward query | Multi-hop fraud network detection |
| Parameterized Cypher | `$accountId` and other query parameters |
| Functional web application | Angular UI |
| Non-technical user workflow | Investigation dashboard and graph exploration |
| Clean UI/UX | Dashboard, investigation views, graph visualization |
| Environment-based secrets | `COGNODB_*` variables |
| Layered architecture | Controller → Service → Repository |
| Database health handling | `/api/health/graph` |
| Hosted demo | Netlify + Render |
| README | This document |
| Screen recording | To be submitted with the repository |

The requirement mapping is based directly on the assignment's data/query, application/UI, engineering, and deliverable sections. fileciteturn1file0L46-L68

---

# 21. Future improvements

The current application intentionally focuses on demonstrating the graph modeling and investigation workflow within the assignment scope.

Potential production improvements include:

- Risk scoring based on multiple graph signals
- Time-window based transaction analysis
- Community/fraud-ring detection
- Transaction velocity analysis
- Investigator case management
- Evidence timelines
- More sophisticated graph algorithms
- Authentication and role-based access
- Pagination for large investigation results
- Audit logging
- Automated alerts for newly detected suspicious networks



# 22. Quick start

For someone reviewing this repository:

```text
1. Create a CognoDB Cloud instance
2. Configure COGNODB_URI
3. Configure COGNODB_USERNAME
4. Configure COGNODB_PASSWORD
5. Start the Spring Boot backend
6. Start the Angular frontend
7. Open the investigation screen
8. Select an account
9. Explore suspicious transaction paths
10. Inspect shared devices/IPs
11. Open the graph visualization
```

The important idea behind FinTrace is simple:

> **Fraud is rarely an isolated transaction. It is often a network of connected accounts, money movements, merchants, devices, and digital signals. FinTrace uses a graph database to make that network visible and queryable.**

---

## Author

**Pavan Porika**

Built as part of the **WEXA AI CognoDB Graph Database Take-Home Assignment**.
