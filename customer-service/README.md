# Customer Service

A gRPC-based microservice responsible for customer onboarding, profile management, and KYC (Know Your Customer) verification within the banking platform.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Business Logic](#business-logic)
4. [Database Schema](#database-schema)
5. [Running the Service](#running-the-service)
6. [gRPC API Reference](#grpc-api-reference)
   - [CustomerService](#customerservice)
   - [CustomerKycService](#customerKycservice)
7. [Testing with grpcurl](#testing-with-grpcurl)
8. [Testing with Postman](#testing-with-postman)

---

## Overview

The `customer-service` exposes two gRPC services on **port 9090**:

| Service             | Proto Package | RPCs |
|---------------------|---------------|------|
| `CustomerService`   | `customer`    | 6    |
| `CustomerKycService`| `kyc`         | 4    |

**Key capabilities:**
- Register new bank customers with auto-generated customer codes (`CUST-00001`)
- Look up customers by internal ID or customer code
- Update customer profile fields and status
- Submit KYC documents for verification
- Approve or reject KYC records; auto-updates the customer's `kyc_verified` flag

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│              customer-service (:9090)            │
│                                                 │
│  CustomerService          CustomerKycService    │
│  ├── CreateCustomer       ├── AddCustomerKyc    │
│  ├── GetCustomer          ├── UpdateKycVerification│
│  ├── GetCustomerByCode    ├── GetCustomerKyc    │
│  ├── UpdateCustomer       └── ListCustomerKyc   │
│  ├── DeleteCustomer                             │
│  └── ListCustomers                              │
│                                                 │
│  Spring Data JPA ──► PostgreSQL (bankingdb)     │
│  Flyway migrations (V1, V2)                     │
└─────────────────────────────────────────────────┘
```

**Inter-service communication:** The service has a gRPC client configured for `ewallet-service` (port 9091) for future account enrichment.

---

## Business Logic

### Customer Onboarding

1. A caller sends `CreateCustomer` with personal details (first name, last name, email, etc.).
2. The service validates that **email** and **id_number** are unique.
3. On save, PostgreSQL automatically generates a `customer_code` using a sequence:
   - Format: `CUST-00001`, `CUST-00002`, … (zero-padded to 5 digits)
4. The new customer starts with `status = ACTIVE` and `kyc_verified = false`.

### Customer Status Lifecycle

```
ACTIVE ──► INACTIVE
  │            │
  └──► BLOCKED ◄┘
         │
       CLOSED
```

| Status     | Meaning                                      |
|------------|----------------------------------------------|
| `ACTIVE`   | Default; customer can transact               |
| `INACTIVE` | Temporarily suspended                        |
| `BLOCKED`  | Access restricted (e.g. compliance hold)     |
| `CLOSED`   | Account permanently closed                   |

### KYC Verification Flow

```
                    ┌── VERIFIED ──► customer.kyc_verified = true
AddCustomerKyc ──► PENDING ──┤
                    └── REJECTED (with rejection_reason)
```

1. **AddCustomerKyc** — Submits a document (passport, national ID, or driving licence) linked to a customer. Status defaults to `PENDING`.
2. **UpdateKycVerification** — A back-office operator sets the status to `VERIFIED` or `REJECTED`:
   - If `VERIFIED`: the parent `customer.kyc_verified` flag is flipped to `true`.
   - If `REJECTED`: a `rejection_reason` must be provided.
3. **GetCustomerKyc** — Retrieves a single KYC record by its ID.
4. **ListCustomerKyc** — Lists all KYC records for a given customer (a customer may submit multiple documents).

### Supported Document Types (`id_type` / `document_type`)

| Enum Value        | Description              |
|-------------------|--------------------------|
| `PASSPORT`        | International passport   |
| `NATIONAL_ID`     | Government-issued ID card|
| `DRIVING_LICENSE` | Driver's licence         |

### Branch Reference

`branch_id` on the customer record is an **optional nullable integer** referencing a branch managed by the `account-service`. No foreign key constraint exists in this service — the branch table lives in a separate microservice.

---

## Database Schema

Schema is managed by **Flyway**. Migrations run automatically on startup.

### V1 — `customers`

```sql
CREATE SEQUENCE IF NOT EXISTS customer_code_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS customers (
    id            BIGSERIAL    PRIMARY KEY,
    customer_code VARCHAR(20)  NOT NULL UNIQUE
                               DEFAULT ('CUST-' || LPAD(NEXTVAL('customer_code_seq')::TEXT, 5, '0')),
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    date_of_birth DATE,
    address       TEXT,
    id_type       VARCHAR(20),
    id_number     VARCHAR(100) UNIQUE,
    kyc_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    branch_id     INTEGER,                          -- nullable, no FK (branch is in account-service)
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### V2 — `customer_kyc`

```sql
CREATE TABLE IF NOT EXISTS customer_kyc (
    id                  BIGSERIAL   PRIMARY KEY,
    customer_id         BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    document_type       VARCHAR(50),
    document_number     VARCHAR(100),
    issue_date          DATE,
    expiry_date         DATE,
    issuing_country     VARCHAR(100),
    issuing_authority   VARCHAR(200),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_at         TIMESTAMPTZ,
    verified_by         VARCHAR(100),
    rejection_reason    TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## Running the Service

### Prerequisites

- Java 21
- Maven
- PostgreSQL running locally (default: `localhost:5432`, database `bankingdb`, user `postgres`, password `root`)

### Environment Variables

| Variable      | Default     | Description          |
|---------------|-------------|----------------------|
| `DB_HOST`     | `localhost` | PostgreSQL host      |
| `DB_PORT`     | `5432`      | PostgreSQL port      |
| `DB_NAME`     | `bankingdb` | Database name        |
| `DB_USER`     | `postgres`  | Database user        |
| `DB_PASSWORD` | `root`      | Database password    |

### Start

```bash
mvn spring-boot:run
```

On startup, Flyway automatically applies any pending migrations. Look for:

```
Successfully applied 2 migrations to schema "public"
```

The gRPC server listens on **port 9090** and the HTTP server on **port 8080**.

---

## gRPC API Reference

### CustomerService

**Proto package:** `customer`
**Java package:** `com.arister.proto`
**gRPC endpoint:** `localhost:9090`

---

#### `CreateCustomer`

Registers a new customer. The `customer_code` is auto-generated by the database.

**Request: `CreateCustomerRequest`**

| Field         | Type           | Required | Description                     |
|---------------|----------------|----------|---------------------------------|
| `firstName`   | string         | Yes      | Customer's first name           |
| `lastName`    | string         | Yes      | Customer's last name            |
| `email`       | string         | Yes      | Unique email address            |
| `phone`       | string         | No       | Phone number                    |
| `dateOfBirth` | string         | No       | ISO date `YYYY-MM-DD`           |
| `address`     | string         | No       | Residential address             |
| `idType`      | `IdType` enum  | No       | `PASSPORT`, `NATIONAL_ID`, `DRIVING_LICENSE` |
| `idNumber`    | string         | No       | Unique document number          |
| `branchId`    | int32          | No       | Branch reference (nullable)     |

**Response: `CustomerResponse`** — returns the created customer with its generated `customer_code`.

---

#### `GetCustomer`

Fetches a customer by internal database ID.

**Request: `GetCustomerRequest`**

| Field | Type  | Required | Description          |
|-------|-------|----------|----------------------|
| `id`  | int64 | Yes      | Customer database ID |

**Response: `CustomerResponse`**

---

#### `GetCustomerByCode`

Fetches a customer by their human-readable customer code (e.g. `CUST-00001`).

**Request: `GetCustomerByCodeRequest`**

| Field          | Type   | Required | Description            |
|----------------|--------|----------|------------------------|
| `customerCode` | string | Yes      | e.g. `CUST-00001`      |

**Response: `CustomerResponse`**

---

#### `UpdateCustomer`

Partially updates a customer's profile. Only non-blank string fields are applied; enum fields (`status`) are always applied.

**Request: `UpdateCustomerRequest`**

| Field       | Type                  | Required | Description               |
|-------------|-----------------------|----------|---------------------------|
| `id`        | int64                 | Yes      | Customer database ID      |
| `firstName` | string                | No       | New first name            |
| `lastName`  | string                | No       | New last name             |
| `email`     | string                | No       | New email                 |
| `phone`     | string                | No       | New phone                 |
| `address`   | string                | No       | New address               |
| `status`    | `CustomerStatus` enum | No       | `ACTIVE`, `INACTIVE`, `BLOCKED`, `CLOSED` |

**Response: `CustomerResponse`**

---

#### `DeleteCustomer`

Permanently deletes a customer and all associated KYC records (cascade).

**Request: `DeleteCustomerRequest`**

| Field | Type  | Required | Description          |
|-------|-------|----------|----------------------|
| `id`  | int64 | Yes      | Customer database ID |

**Response: `DeleteCustomerResponse`** — `{ success: true, message: "..." }`

---

#### `ListCustomers`

Returns a paginated list of all customers.

**Request: `ListCustomersRequest`**

| Field  | Type  | Default | Description              |
|--------|-------|---------|--------------------------|
| `page` | int32 | `0`     | Zero-based page number   |
| `size` | int32 | `20`    | Number of items per page |

**Response: `ListCustomersResponse`** — includes `customers[]`, `total_count`, `page`, `size`.

---

### CustomerKycService

**Proto package:** `kyc`
**Java package:** `com.arister.proto`
**gRPC endpoint:** `localhost:9090`

---

#### `AddCustomerKyc`

Submits a KYC document for a customer. The record is created with `verification_status = PENDING`.

**Request: `AddCustomerKycRequest`**

| Field              | Type   | Required | Description                     |
|--------------------|--------|----------|---------------------------------|
| `customerId`       | int64  | Yes      | ID of the customer              |
| `documentType`     | string | No       | e.g. `PASSPORT`, `NATIONAL_ID`  |
| `documentNumber`   | string | No       | Document identifier             |
| `issueDate`        | string | No       | ISO date `YYYY-MM-DD`           |
| `expiryDate`       | string | No       | ISO date `YYYY-MM-DD`           |
| `issuingCountry`   | string | No       | e.g. `Malaysia`                 |
| `issuingAuthority` | string | No       | e.g. `Jabatan Imigresen Malaysia`|
| `notes`            | string | No       | Internal notes                  |

**Response: `CustomerKycResponse`** — returns the created KYC record with `PENDING` status.

---

#### `UpdateKycVerification`

Updates the verification status of a KYC record. If set to `VERIFIED`, the parent customer's `kyc_verified` flag is automatically set to `true`.

**Request: `UpdateKycVerificationRequest`**

| Field                | Type                       | Required | Description                        |
|----------------------|----------------------------|----------|------------------------------------|
| `kycId`              | int64                      | Yes      | KYC record ID                      |
| `verificationStatus` | `KycVerificationStatus`    | Yes      | `VERIFIED` or `REJECTED`           |
| `verifiedBy`         | string                     | No       | Operator name/ID                   |
| `rejectionReason`    | string                     | No       | Required when status is `REJECTED` |

**Response: `CustomerKycResponse`**

---

#### `GetCustomerKyc`

Fetches a single KYC record by its ID.

**Request: `GetCustomerKycRequest`**

| Field   | Type  | Required | Description    |
|---------|-------|----------|----------------|
| `kycId` | int64 | Yes      | KYC record ID  |

**Response: `CustomerKycResponse`**

---

#### `ListCustomerKyc`

Lists all KYC records for a given customer.

**Request: `ListCustomerKycRequest`**

| Field        | Type  | Required | Description          |
|--------------|-------|----------|----------------------|
| `customerId` | int64 | Yes      | Customer database ID |

**Response: `ListCustomerKycResponse`** — includes `kyc_records[]`.

---

## Testing with grpcurl

Install grpcurl: https://github.com/fullstorydev/grpcurl#installation

### CustomerService

**Create a customer**

```bash
grpcurl -plaintext -d '{
  "firstName": "Ahmad",
  "lastName": "Razif",
  "email": "ahmad.razif@example.com",
  "phone": "+60123456789",
  "dateOfBirth": "1990-05-15",
  "address": "No. 12, Jalan Merdeka, Kuala Lumpur",
  "idType": "NATIONAL_ID",
  "idNumber": "900515-14-5678",
  "branchId": 1
}' localhost:9090 customer.CustomerService/CreateCustomer
```

**Get customer by ID**

```bash
grpcurl -plaintext -d '{"id": 1}' \
  localhost:9090 customer.CustomerService/GetCustomer
```

**Get customer by code**

```bash
grpcurl -plaintext -d '{"customerCode": "CUST-00001"}' \
  localhost:9090 customer.CustomerService/GetCustomerByCode
```

**Update customer**

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "phone": "+60199999999",
  "address": "No. 5, Jalan Ampang, Kuala Lumpur",
  "status": "ACTIVE"
}' localhost:9090 customer.CustomerService/UpdateCustomer
```

**Block a customer**

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "status": "BLOCKED"
}' localhost:9090 customer.CustomerService/UpdateCustomer
```

**List customers (first page, 10 per page)**

```bash
grpcurl -plaintext -d '{"page": 0, "size": 10}' \
  localhost:9090 customer.CustomerService/ListCustomers
```

**Delete a customer**

```bash
grpcurl -plaintext -d '{"id": 1}' \
  localhost:9090 customer.CustomerService/DeleteCustomer
```

---

### CustomerKycService

**Submit a KYC document**

```bash
grpcurl -plaintext -d '{
  "customerId": 1,
  "documentType": "PASSPORT",
  "documentNumber": "A12345678",
  "issueDate": "2020-01-10",
  "expiryDate": "2030-01-09",
  "issuingCountry": "Malaysia",
  "issuingAuthority": "Jabatan Imigresen Malaysia",
  "notes": "Submitted via mobile app"
}' localhost:9090 kyc.CustomerKycService/AddCustomerKyc
```

**Approve a KYC record**

```bash
grpcurl -plaintext -d '{
  "kycId": 1,
  "verificationStatus": "VERIFIED",
  "verifiedBy": "ops-officer-001"
}' localhost:9090 kyc.CustomerKycService/UpdateKycVerification
```

**Reject a KYC record**

```bash
grpcurl -plaintext -d '{
  "kycId": 2,
  "verificationStatus": "REJECTED",
  "verifiedBy": "ops-officer-001",
  "rejectionReason": "Document image is blurry and unreadable"
}' localhost:9090 kyc.CustomerKycService/UpdateKycVerification
```

**Get a KYC record by ID**

```bash
grpcurl -plaintext -d '{"kycId": 1}' \
  localhost:9090 kyc.CustomerKycService/GetCustomerKyc
```

**List all KYC records for a customer**

```bash
grpcurl -plaintext -d '{"customerId": 1}' \
  localhost:9090 kyc.CustomerKycService/ListCustomerKyc
```

**List available services and methods**

```bash
# List all services
grpcurl -plaintext localhost:9090 list

# List methods of CustomerService
grpcurl -plaintext localhost:9090 list customer.CustomerService

# List methods of CustomerKycService
grpcurl -plaintext localhost:9090 list kyc.CustomerKycService

# Describe a method
grpcurl -plaintext localhost:9090 describe customer.CustomerService.CreateCustomer
```

---

## Testing with Postman

Postman supports gRPC natively (version 10+).

### Setup

1. Open Postman → click **New** → select **gRPC Request**.
2. Enter the server URL: `localhost:9090`
3. Click **Import a .proto file** and import:
   - `src/main/proto/customer.proto` for `CustomerService`
   - `src/main/proto/kyc.proto` for `CustomerKycService`
4. Select the service and method from the dropdown.
5. Set the request body as JSON and click **Invoke**.

### Example — CreateCustomer in Postman

- **URL:** `localhost:9090`
- **Service:** `customer.CustomerService`
- **Method:** `CreateCustomer`
- **Message body:**

```json
{
  "firstName": "Ahmad",
  "lastName": "Razif",
  "email": "ahmad.razif@example.com",
  "phone": "+60123456789",
  "dateOfBirth": "1990-05-15",
  "address": "No. 12, Jalan Merdeka, Kuala Lumpur",
  "idType": "NATIONAL_ID",
  "idNumber": "900515-14-5678",
  "branchId": 1
}
```

### Example — AddCustomerKyc in Postman

- **URL:** `localhost:9090`
- **Service:** `kyc.CustomerKycService`
- **Method:** `AddCustomerKyc`
- **Message body:**

```json
{
  "customerId": 1,
  "documentType": "PASSPORT",
  "documentNumber": "A12345678",
  "issueDate": "2020-01-10",
  "expiryDate": "2030-01-09",
  "issuingCountry": "Malaysia",
  "issuingAuthority": "Jabatan Imigresen Malaysia"
}
```

### Enum Values in Postman

Pass enum values as their **string names** in the JSON body:

| Field                 | Valid values                                    |
|-----------------------|-------------------------------------------------|
| `id_type`             | `"PASSPORT"`, `"NATIONAL_ID"`, `"DRIVING_LICENSE"` |
| `status`              | `"ACTIVE"`, `"INACTIVE"`, `"BLOCKED"`, `"CLOSED"` |
| `verification_status` | `"PENDING"`, `"VERIFIED"`, `"REJECTED"`         |
