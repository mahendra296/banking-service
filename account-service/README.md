# Account Service

A gRPC microservice handling bank branches, accounts, transactions, fund transfers, and beneficiary management. Runs on **HTTP port 8081** and **gRPC port 9091**.

---

## Services Overview

| Service              | RPCs | Description                                |
|----------------------|------|--------------------------------------------|
| `BranchService`      | 6    | Bank branch CRUD                           |
| `AccountService`     | 7    | Account lifecycle management               |
| `TransactionService` | 4    | Deposits, withdrawals, transaction history |
| `TransferService`    | 3    | Fund transfers between accounts            |
| `BeneficiaryService` | 5    | Saved payee management                     |

---

## Database Migrations

This service shares `bankingdb` with `customer-service`. Migrations are applied in sequence:

| Version | File                                  | Description               |
|---------|---------------------------------------|---------------------------|
| V3      | `V3__create_branches_table.sql`       | Branches table            |
| V4      | `V4__create_accounts_table.sql`       | Accounts table + sequence |
| V5      | `V5__create_transactions_table.sql`   | Transactions table        |
| V6      | `V6__create_transfers_table.sql`      | Transfers table           |
| V7      | `V7__create_beneficiaries_table.sql`  | Beneficiaries table       |

> **Note:** Start `customer-service` first so the `customers` table (V1) exists before account-service applies V4 (which has a FK to `customers`).

---

## Running Locally

```bash
# Start customer-service first, then:
cd account-service
mvn spring-boot:run
```

The service connects to PostgreSQL at `localhost:5432/bankingdb` by default.

Environment variable overrides: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

---

## gRPC API Reference

### Prerequisites for grpcurl

```bash
# Install grpcurl
# Windows: scoop install grpcurl
# macOS:   brew install grpcurl
# Linux:   https://github.com/fullstorydev/grpcurl/releases

# List all available services
grpcurl -plaintext localhost:9091 list

# Describe a service
grpcurl -plaintext localhost:9091 describe branch.BranchService
```

---

## BranchService

Manages bank branch records.

### CreateBranch

```bash
grpcurl -plaintext -d '{
  "branchCode": "BR-001",
  "branchName": "Main Street Branch",
  "city": "New York",
  "state": "NY",
  "phone": "+1-212-555-0100"
}' localhost:9091 branch.BranchService/CreateBranch
```

**Response:**
```json
{
  "success": true,
  "message": "Branch created successfully",
  "branch": {
    "id": "1",
    "branchCode": "BR-001",
    "branchName": "Main Street Branch",
    "city": "New York",
    "state": "NY",
    "phone": "+1-212-555-0100",
    "isActive": true,
    "createdAt": "2026-02-19T10:00:00Z"
  }
}
```

### GetBranch

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 branch.BranchService/GetBranch
```

### GetBranchByCode

```bash
grpcurl -plaintext -d '{"branchCode": "BR-001"}' localhost:9091 branch.BranchService/GetBranchByCode
```

### UpdateBranch

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "branchName": "Main Street Branch (Updated)",
  "city": "New York",
  "state": "NY",
  "phone": "+1-212-555-0199",
  "isActive": true
}' localhost:9091 branch.BranchService/UpdateBranch
```

### DeleteBranch

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 branch.BranchService/DeleteBranch
```

### ListBranches

```bash
grpcurl -plaintext -d '{"page": 0, "size": 10}' localhost:9091 branch.BranchService/ListBranches
```

**Response:**
```json
{
  "success": true,
  "message": "Branches retrieved successfully",
  "branches": [...],
  "totalCount": 5,
  "page": 0,
  "size": 10
}
```

---

## AccountService

Manages bank accounts. Account numbers are auto-generated (e.g., `ACC-1000000001`).

### AccountType values

| Value           | Description         |
|-----------------|---------------------|
| `SAVINGS`       | Savings account (0) |
| `CURRENT`       | Current account (1) |
| `FIXED_DEPOSIT` | Fixed deposit (2)   |

### AccountStatus values

| Value     | Description          |
|-----------|----------------------|
| `ACTIVE`  | Active account (0)   |
| `DORMANT` | Dormant account (1)  |
| `FROZEN`  | Frozen account (2)   |
| `CLOSED`  | Closed account (3)   |

### CreateAccount

```bash
grpcurl -plaintext -d '{
  "customerId": 1,
  "branchId": 1,
  "accountType": "SAVINGS",
  "currency": "USD",
  "interestRate": "3.50",
  "minBalance": "500.00",
  "overdraftLimit": "0.00"
}' localhost:9091 account.AccountService/CreateAccount
```

**Response:**
```json
{
  "success": true,
  "message": "Account created successfully",
  "account": {
    "id": "1",
    "accountNumber": "ACC-1000000001",
    "customerId": "1",
    "branchId": "1",
    "accountType": "SAVINGS",
    "balance": "0.00",
    "currency": "USD",
    "interestRate": "3.50",
    "minBalance": "500.00",
    "overdraftLimit": "0.00",
    "status": "ACTIVE",
    "createdAt": "2026-02-19T10:00:00Z"
  }
}
```

### GetAccount

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 account.AccountService/GetAccount
```

### GetAccountByNumber

```bash
grpcurl -plaintext -d '{"accountNumber": "ACC-1000000001"}' localhost:9091 account.AccountService/GetAccountByNumber
```

### UpdateAccount

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "interestRate": "4.00",
  "minBalance": "1000.00",
  "overdraftLimit": "500.00",
  "status": "ACTIVE"
}' localhost:9091 account.AccountService/UpdateAccount
```

### DeleteAccount

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 account.AccountService/DeleteAccount
```

### ListAccounts

```bash
grpcurl -plaintext -d '{"page": 0, "size": 20}' localhost:9091 account.AccountService/ListAccounts
```

### ListAccountsByCustomer

```bash
grpcurl -plaintext -d '{"customerId": 1}' localhost:9091 account.AccountService/ListAccountsByCustomer
```

---

## TransactionService

Records deposits, withdrawals, and provides transaction history. Each transaction stores `balanceBefore` and `balanceAfter` for auditability. Transaction references are auto-generated (e.g., `TXN-20260219-00000001`).

### TransactionType values

| Value          | Description                 |
|----------------|-----------------------------|
| `DEPOSIT`      | Cash/cheque deposit (0)     |
| `WITHDRAWAL`   | Cash withdrawal (1)         |
| `TRANSFER_IN`  | Inbound fund transfer (2)   |
| `TRANSFER_OUT` | Outbound fund transfer (3)  |
| `INTEREST`     | Interest credit (4)         |
| `FEE`          | Service charge/fee (5)      |
| `REVERSAL`     | Transaction reversal (6)    |

### Deposit

```bash
grpcurl -plaintext -d '{
  "accountId": 1,
  "amount": "5000.00",
  "description": "Initial deposit",
  "performedBy": 1
}' localhost:9091 transaction.TransactionService/Deposit
```

**Response:**
```json
{
  "success": true,
  "message": "Deposit successful",
  "transaction": {
    "id": "1",
    "transactionRef": "TXN-20260219-00000001",
    "accountId": "1",
    "transactionType": "DEPOSIT",
    "amount": "5000.00",
    "balanceBefore": "0.00",
    "balanceAfter": "5000.00",
    "description": "Initial deposit",
    "performedBy": "1",
    "createdAt": "2026-02-19T10:00:00Z"
  }
}
```

### Withdraw

```bash
grpcurl -plaintext -d '{
  "accountId": 1,
  "amount": "200.00",
  "description": "ATM withdrawal",
  "performedBy": 1
}' localhost:9091 transaction.TransactionService/Withdraw
```

> Withdrawal fails if `balance - amount < minBalance - overdraftLimit`.

### GetTransaction

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 transaction.TransactionService/GetTransaction
```

### ListTransactionsByAccount

```bash
grpcurl -plaintext -d '{
  "accountId": 1,
  "page": 0,
  "size": 20
}' localhost:9091 transaction.TransactionService/ListTransactionsByAccount
```

**Response:**
```json
{
  "success": true,
  "message": "Transactions retrieved successfully",
  "transactions": [...],
  "totalCount": 42,
  "page": 0,
  "size": 20
}
```

---

## TransferService

Transfers funds between two accounts atomically. Creates a `TRANSFER_OUT` transaction on the source account and a `TRANSFER_IN` transaction on the destination account in a single database transaction. Transfer references are auto-generated (e.g., `TRF-20260219-00000001`).

### TransferStatus values

| Value       | Description              |
|-------------|--------------------------|
| `PENDING`   | Transfer pending (0)     |
| `COMPLETED` | Transfer completed (1)   |
| `FAILED`    | Transfer failed (2)      |
| `REVERSED`  | Transfer reversed (3)    |

### CreateTransfer

```bash
grpcurl -plaintext -d '{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": "1000.00",
  "fee": "5.00",
  "description": "Rent payment"
}' localhost:9091 transfer.TransferService/CreateTransfer
```

**Response:**
```json
{
  "success": true,
  "message": "Transfer completed successfully",
  "transfer": {
    "id": "1",
    "transferRef": "TRF-20260219-00000001",
    "fromAccountId": "1",
    "toAccountId": "2",
    "amount": "1000.00",
    "fee": "5.00",
    "status": "COMPLETED",
    "description": "Rent payment",
    "createdAt": "2026-02-19T10:00:00Z"
  }
}
```

### GetTransfer

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 transfer.TransferService/GetTransfer
```

### ListTransfersByAccount

```bash
grpcurl -plaintext -d '{
  "accountId": 1,
  "page": 0,
  "size": 20
}' localhost:9091 transfer.TransferService/ListTransfersByAccount

```

---

## BeneficiaryService

Manages saved payees (beneficiaries) for a customer. A beneficiary can be an account in the same bank (use `bankName: "SAME_BANK"`) or at an external bank (provide `bankName` + `ifscCode`).

### AddBeneficiary

```bash
grpcurl -plaintext -d '{
  "customerId": 1,
  "beneficiaryName": "John Doe",
  "accountNumber": "ACC-1000000002",
  "bankName": "SAME_BANK",
  "ifscCode": ""
}' localhost:9091 beneficiary.BeneficiaryService/AddBeneficiary
```

**Response:**
```json
{
  "success": true,
  "message": "Beneficiary added successfully",
  "beneficiary": {
    "id": "1",
    "customerId": "1",
    "beneficiaryName": "John Doe",
    "accountNumber": "ACC-1000000002",
    "bankName": "SAME_BANK",
    "ifscCode": "",
    "isVerified": false,
    "createdAt": "2026-02-19T10:00:00Z"
  }
}
```

### Add External Bank Beneficiary

```bash
grpcurl -plaintext -d '{
  "customerId": 1,
  "beneficiaryName": "Jane Smith",
  "accountNumber": "9876543210",
  "bankName": "HDFC Bank",
  "ifscCode": "HDFC0001234"
}' localhost:9091 beneficiary.BeneficiaryService/AddBeneficiary
```

### GetBeneficiary

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 beneficiary.BeneficiaryService/GetBeneficiary
```

### UpdateBeneficiary

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "beneficiaryName": "John Doe Jr.",
  "bankName": "SAME_BANK",
  "ifscCode": "",
  "isVerified": true
}' localhost:9091 beneficiary.BeneficiaryService/UpdateBeneficiary
```

### RemoveBeneficiary

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9091 beneficiary.BeneficiaryService/RemoveBeneficiary
```

### ListBeneficiaries

```bash
grpcurl -plaintext -d '{"customerId": 1}' localhost:9091 beneficiary.BeneficiaryService/ListBeneficiaries
```

**Response:**
```json
{
  "success": true,
  "message": "Beneficiaries retrieved successfully",
  "beneficiaries": [
    {
      "id": "1",
      "customerId": "1",
      "beneficiaryName": "John Doe",
      "accountNumber": "ACC-1000000002",
      "bankName": "SAME_BANK",
      "isVerified": false,
      "createdAt": "2026-02-19T10:00:00Z"
    }
  ]
}
```

---

## Using Postman

1. Open Postman and create a **New Request**
2. Set request type to **gRPC**
3. Enter server URL: `localhost:9091`
4. Click **Import a .proto file** and import the relevant proto file from `src/main/proto/`
5. Select the service and method from the dropdown
6. Enter the request JSON body and click **Invoke**

---

## Error Handling

All RPCs return a response with `success: false` and a descriptive `message` on failure:

```json
{
  "success": false,
  "message": "Account not found with id: 999"
}
```

Common error cases:
- Account/branch/entity not found → `success: false`
- Insufficient balance for withdrawal → `success: false`
- Transfer between same account → `success: false`
- Duplicate account number or branch code → `success: false`
