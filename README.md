# Banking Service

A microservices-based banking platform built with **Spring Boot 4**, **gRPC**, and **PostgreSQL**. The platform handles customer onboarding, KYC verification, bank account management, transactions, fund transfers, and beneficiary management.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Services](#services)
   - [common-service](#common-service)
   - [customer-service](#customer-service)
   - [account-service](#account-service)
3. [Tech Stack](#tech-stack)
4. [Database](#database)
5. [Prerequisites](#prerequisites)
6. [Startup Order](#startup-order)
7. [Environment Variables](#environment-variables)
8. [Service Ports](#service-ports)
9. [gRPC API Summary](#grpc-api-summary)

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        banking-service (mono-repo)               │
│                                                                  │
│  ┌─────────────────┐   gRPC client    ┌──────────────────────┐  │
│  │ customer-service│ ──────────────► │   account-service    │  │
│  │   :8080 / :9090 │                 │    :8081 / :9091     │  │
│  └────────┬────────┘                 └──────────┬───────────┘  │
│           │                                     │              │
│           └──────────────┬──────────────────────┘              │
│                          ▼                                      │
│                  PostgreSQL (bankingdb)                          │
│                      :5432                                      │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    common-service                        │   │
│  │   (shared library — BaseEntity, DTOs, exceptions)        │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Services

### common-service

**Type:** Shared library (JAR, not a runnable service)
**Artifact:** `com.arister:common-service:1.0.1-SNAPSHOT`

A shared Maven library consumed by all other services. It provides:

| Component               | Description                                                                 |
|-------------------------|-----------------------------------------------------------------------------|
| `BaseEntity`            | JPA `@MappedSuperclass` with `id`, `createdAt`, `updatedAt` + lifecycle hooks |
| `ApiResponse<T>`        | Standardised HTTP response wrapper                                          |
| `PageResponseDTO<T>`    | Pagination response envelope                                                |
| `PageableDTO`           | Pagination request parameters                                               |
| `BusinessException`     | Domain-level checked exception carrying an `ErrorCode`                      |
| `ResourceNotFoundException` | Thrown when a requested entity does not exist                           |
| `GlobalExceptionHandler`| Spring `@ControllerAdvice` that maps exceptions to HTTP responses           |
| `CommonErrorCode`       | Enum of standard error codes                                                |
| `GeneralUtils`          | General-purpose utility helpers                                             |

---

### customer-service

**README:** [`customer-service/README.md`](customer-service/README.md)
**HTTP port:** `8080` | **gRPC port:** `9090`
**Database migrations:** V1, V2 (runs first)

Handles customer onboarding, profile management, and KYC (Know Your Customer) verification.

#### gRPC Services

| Service              | RPCs | Description                                          |
|----------------------|------|------------------------------------------------------|
| `CustomerService`    | 6    | Create, read, update, delete, and list customers     |
| `CustomerKycService` | 4    | Submit KYC documents and manage verification status  |

#### Key Features

- **Customer registration** with auto-generated customer codes (`CUST-00001`, `CUST-00002`, …)
- **Unique constraints** on email and national ID number
- **Customer status lifecycle:** `ACTIVE` → `INACTIVE` / `BLOCKED` → `CLOSED`
- **KYC workflow:** document submission (`PENDING`) → back-office review → `VERIFIED` / `REJECTED`
- Approving a KYC record automatically sets `customer.kyc_verified = true`
- Supports document types: `PASSPORT`, `NATIONAL_ID`, `DRIVING_LICENSE`

#### CustomerService RPCs

| RPC                  | Description                                   |
|----------------------|-----------------------------------------------|
| `CreateCustomer`     | Register a new customer                       |
| `GetCustomer`        | Fetch by internal ID                          |
| `GetCustomerByCode`  | Fetch by customer code (e.g. `CUST-00001`)    |
| `UpdateCustomer`     | Partial update of profile fields and status   |
| `DeleteCustomer`     | Permanently delete customer + KYC (cascade)   |
| `ListCustomers`      | Paginated list of all customers               |

#### CustomerKycService RPCs

| RPC                      | Description                                         |
|--------------------------|-----------------------------------------------------|
| `AddCustomerKyc`         | Submit a KYC document (starts as `PENDING`)         |
| `UpdateKycVerification`  | Approve or reject a KYC record                      |
| `GetCustomerKyc`         | Fetch a single KYC record by ID                     |
| `ListCustomerKyc`        | List all KYC records for a customer                 |

#### Database Tables

| Table           | Migration | Description                          |
|-----------------|-----------|--------------------------------------|
| `customers`     | V1        | Core customer profiles               |
| `customer_kyc`  | V2        | KYC document records per customer    |

---

### account-service

**README:** [`account-service/README.md`](account-service/README.md)
**HTTP port:** `8081` | **gRPC port:** `9091`
**Database migrations:** V3 – V7 (requires V1, V2 from customer-service)

Handles bank branches, accounts, financial transactions, fund transfers, and beneficiary management.

#### gRPC Services

| Service              | RPCs | Description                                        |
|----------------------|------|----------------------------------------------------|
| `BranchService`      | 6    | Bank branch CRUD                                   |
| `AccountService`     | 7    | Account lifecycle and balance management           |
| `TransactionService` | 4    | Deposits, withdrawals, and transaction history     |
| `TransferService`    | 3    | Atomic fund transfers between accounts             |
| `BeneficiaryService` | 5    | Saved payee (beneficiary) management               |

#### BranchService RPCs

| RPC               | Description                                 |
|-------------------|---------------------------------------------|
| `CreateBranch`    | Create a new branch record                  |
| `GetBranch`       | Fetch branch by internal ID                 |
| `GetBranchByCode` | Fetch branch by code (e.g. `BR-001`)        |
| `UpdateBranch`    | Update branch details or active status      |
| `DeleteBranch`    | Delete a branch                             |
| `ListBranches`    | Paginated list of all branches              |

#### AccountService RPCs

| RPC                      | Description                                              |
|--------------------------|----------------------------------------------------------|
| `CreateAccount`          | Open a new bank account (auto-generates account number)  |
| `GetAccount`             | Fetch by internal ID                                     |
| `GetAccountByNumber`     | Fetch by account number (e.g. `ACC-1000000001`)          |
| `UpdateAccount`          | Update interest rate, limits, or account status          |
| `DeleteAccount`          | Delete an account                                        |
| `ListAccounts`           | Paginated list of all accounts                           |
| `ListAccountsByCustomer` | All accounts belonging to a specific customer            |

Account types: `SAVINGS`, `CURRENT`, `FIXED_DEPOSIT`
Account statuses: `ACTIVE`, `DORMANT`, `FROZEN`, `CLOSED`

#### TransactionService RPCs

| RPC                        | Description                                                    |
|----------------------------|----------------------------------------------------------------|
| `Deposit`                  | Credit funds to an account; updates balance atomically         |
| `Withdraw`                 | Debit funds; enforces overdraft limit check                    |
| `GetTransaction`           | Fetch a single transaction by ID                               |
| `ListTransactionsByAccount`| Paginated transaction history for an account                   |

Each transaction stores `balanceBefore` and `balanceAfter` for full auditability. Transaction references are auto-generated (`TXN-YYYYMMDD-00000001`).

Transaction types: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`, `INTEREST`, `FEE`, `REVERSAL`

#### TransferService RPCs

| RPC                      | Description                                                               |
|--------------------------|---------------------------------------------------------------------------|
| `CreateTransfer`         | Atomically debit source and credit destination; creates two transactions  |
| `GetTransfer`            | Fetch a single transfer by ID                                             |
| `ListTransfersByAccount` | Paginated transfer history for an account                                 |

Transfer references are auto-generated (`TRF-YYYYMMDD-00000001`).
Transfer statuses: `PENDING`, `COMPLETED`, `FAILED`, `REVERSED`

#### BeneficiaryService RPCs

| RPC                  | Description                                         |
|----------------------|-----------------------------------------------------|
| `AddBeneficiary`     | Save a payee (same-bank or external bank)           |
| `GetBeneficiary`     | Fetch by internal ID                                |
| `UpdateBeneficiary`  | Update payee name, bank, or verified status         |
| `RemoveBeneficiary`  | Delete a saved payee                                |
| `ListBeneficiaries`  | All beneficiaries for a given customer              |

#### Database Tables

| Table           | Migration | Description                                     |
|-----------------|-----------|-------------------------------------------------|
| `branches`      | V3        | Bank branch records                             |
| `accounts`      | V4        | Bank accounts with FK to `customers`            |
| `transactions`  | V5        | Transaction ledger with auto-generated ref      |
| `transfers`     | V6        | Fund transfer records with auto-generated ref   |
| `beneficiaries` | V7        | Saved payees per customer                       |

---

## Tech Stack

| Layer             | Technology                                    |
|-------------------|-----------------------------------------------|
| Language          | Java 21                                       |
| Framework         | Spring Boot 4.0.1                             |
| RPC               | gRPC (grpc-spring-boot-starter 3.1.0)         |
| ORM               | Spring Data JPA / Hibernate 6                 |
| Database          | PostgreSQL                                    |
| Migrations        | Flyway                                        |
| Serialisation     | Protocol Buffers (protobuf 4.28.3)            |
| Build             | Maven                                         |
| Code generation   | Lombok, protobuf-maven-plugin                 |
| Code formatting   | Spotless + Palantir Java Format               |

---

## Database

Both services share a single PostgreSQL database (`bankingdb`). Flyway migrations are split between the two services and must run in version order:

| Version | Owned by           | Table created    |
|---------|--------------------|------------------|
| V1      | customer-service   | `customers`      |
| V2      | customer-service   | `customer_kyc`   |
| V3      | account-service    | `branches`       |
| V4      | account-service    | `accounts`       |
| V5      | account-service    | `transactions`   |
| V6      | account-service    | `transfers`      |
| V7      | account-service    | `beneficiaries`  |

Auto-generated reference columns (`customer_code`, `account_number`, `transaction_ref`, `transfer_ref`) are driven by PostgreSQL sequences and use Hibernate's `@Generated(event = EventType.INSERT)` to populate the field in the returned entity after save.

---

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL (running on `localhost:5432`)
- `grpcurl` for API testing ([installation guide](https://github.com/fullstorydev/grpcurl#installation))

Create the database before starting any service:

```sql
CREATE DATABASE bankingdb;
```

---

## Startup Order

`customer-service` **must start first** because `account-service` migration V4 creates the `accounts` table with a foreign key referencing `customers.id`.

```bash
# 1. Start customer-service
cd customer-service
mvn spring-boot:run

# 2. Start account-service (in a separate terminal)
cd account-service
mvn spring-boot:run
```

---

## Environment Variables

Both services accept the same database environment variables:

| Variable      | Default     | Description       |
|---------------|-------------|-------------------|
| `DB_HOST`     | `localhost` | PostgreSQL host   |
| `DB_PORT`     | `5432`      | PostgreSQL port   |
| `DB_NAME`     | `bankingdb` | Database name     |
| `DB_USER`     | `postgres`  | Database user     |
| `DB_PASSWORD` | `root`      | Database password |

---

## Service Ports

| Service          | HTTP port | gRPC port |
|------------------|-----------|-----------|
| customer-service | `8080`    | `9090`    |
| account-service  | `8081`    | `9091`    |

---

## gRPC API Summary

| Service              | Endpoint         | RPCs |
|----------------------|------------------|------|
| `CustomerService`    | `localhost:9090` | 6    |
| `CustomerKycService` | `localhost:9090` | 4    |
| `BranchService`      | `localhost:9091` | 6    |
| `AccountService`     | `localhost:9091` | 7    |
| `TransactionService` | `localhost:9091` | 4    |
| `TransferService`    | `localhost:9091` | 3    |
| `BeneficiaryService` | `localhost:9091` | 5    |

Full request/response schemas and `grpcurl` examples are documented in each service's README:

- [`customer-service/README.md`](customer-service/README.md)
- [`account-service/README.md`](account-service/README.md)
