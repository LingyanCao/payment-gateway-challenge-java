# Payment Gateway - Design Document

## Overview

This document describes the design and implementation of a Payment Gateway service. The gateway acts as an intermediary between merchants and banks, providing a secure and reliable payment processing API.

## Architecture

### High-Level Architecture

```
Merchant (Client)  ←→  Payment Gateway  ←→  Bank Simulator
                             ↓
                       In-Memory Repository
```

## API Design

### 1. Process Payment

**Endpoint**: `POST /payments`

**Purpose**: Processes a new payment transaction by forwarding the request to the bank and storing the result.

**Request Body**:
```json
{
  "card_number": "2222405343248877",
  "expiry_month": 4,
  "expiry_year": 2026,
  "currency": "GBP",
  "amount": 100,
  "cvv": 123
}
```

**Response** (Success - 200 OK):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "Authorized",
  "cardNumberLastFour": 8877,
  "expiryMonth": 4,
  "expiryYear": 2026,
  "currency": "GBP",
  "amount": 100
}
```

**Response** (Bank Unavailable - 503 Service Unavailable):
```json
{
  "message": "Payment service temporarily unavailable. Please try again later."
}
```

**Response** (Validation Error - 400 Bad Request):
```json
{
  "error": "Validation failed",
  "errors": {
    "cardNumber": "Card number must be between 14-19 digits",
    "amount": "Amount must be greater than 0"
  }
}
```

### 2. Retrieve Payment

**Endpoint**: `GET /payments/{id}`

**Purpose**: Retrieves the details of a previously processed payment.

**Response** (Success - 200 OK):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "Authorized",
  "cardNumberLastFour": 8877,
  "expiryMonth": 4,
  "expiryYear": 2026,
  "currency": "GBP",
  "amount": 100
}
```

**Response** (Not Found - 404):
```json
{
  "message": "Payment not found. Please check the payment ID and try again."
}
```

## Component Design

### 1. Controller Layer (`PaymentGatewayController`)

**Responsibilities**:
- Expose RESTful API endpoints
- Handle HTTP request/response mapping
- Trigger request validation via `@Valid` annotation
- Delegate business logic to service layer

**Endpoints**:
- `POST /payments` - Process payment
- `GET /payments/{id}` - Retrieve payment by ID

### 2. Service Layer (`PaymentGatewayService`)

**Responsibilities**:
- Implement core business logic
- Coordinate between BankClient and Storage Repository
- Handle error scenarios and exceptions

**Payment Processing Flow**:
1. Generate unique payment ID
2. Transform request to bank-compatible format
3. Call bank service via BankClient
4. Determine payment status from bank response
5. Store payment record in repository
6. Return payment response to controller

**Error Handling**:

**POST /payments (Process Payment)**:
- **Bank 5xx Errors**: Throw `EventProcessingException` with 503 status code (no payment stored)
- **Other Bank Service Failures**: Throw `EventProcessingException` with 503 status code (no payment stored)
- **Bank Declines**: Store payment with `DECLINED` status and return 200 OK with response
- **Bank Authorized**: Store payment with `Authorized` status and return 200 OK with response
- **Validation Errors**: Return 400 Bad Request with detailed error messages

**GET /payments/{id} (Retrieve Payment)**:
- **Payment Not Found**: Throw `EventProcessingException` with 404 status code
- **Payment Found**: Return 200 OK with payment details

**Design Decision**: 
- **POST endpoint**: Technical failures (bank server errors, network issues) do NOT create payment records because the actual payment status is unknown. Only business decisions (authorized/declined) are stored.
- **GET endpoint**: Only validates that the payment ID exists in the repository. No external service calls.

### 3. Client Layer (`BankClient`)

**Responsibilities**:
- Abstract bank service communication
- Transform internal models to bank API format
- Handle HTTP communication via RestTemplate

**Configuration**:
- Bank URL: Configurable via `bank.simulator.url` property (default: `http://localhost:8080`)
- Endpoint: `POST /payments`

**Request/Response**:
- Maps `PostPaymentRequest` to `BankRequest` format
- Receives `BankResponse` with authorization status
- Propagates HTTP exceptions to service layer

### 4. Repository Layer (`PaymentsRepository`)

**Responsibilities**:
- Store and retrieve payment records
- Provide in-memory storage implementation

## Validation Design

The payment gateway implements a comprehensive validation strategy using two-tier approach: field-level validation and class-level validation. Do the full validation in payment gateway layer to avoid making unnecessary bank service call.

### Field-Level Validation
 `PostPaymentRequest`:

| Field | Constraints | Validation Rules | Error Message |
|-------|------------|------------------|---------------|
| `card_number` | `@NotNull`, `@Pattern(^[0-9]{14,19}$)` | Required, 14-19 numeric digits only | "Card number must be between 14-19 digits" |
| `expiry_month` | `@NotNull`, `@Min(1)`, `@Max(12)` | Required, range 1-12 | "Expiry month must be between 1 and 12" |
| `expiry_year` | `@NotNull`, `@Min(2026)` | Required, minimum 2026 | "Expiry year must be 2026 or later" |
| `currency` | `@NotNull`, `@Pattern(^(GBP\|USD\|CNY)$)` | Required, whitelist validation | "Currency must be one of: GBP, USD, CNY" |
| `amount` | `@NotNull`, `@Positive` | Required, must be > 0 | "Amount must be greater than 0" |
| `cvv` | `@NotNull`, `@Min(100)`, `@Max(9999)` | Required, 3-4 digits (100-9999) | "CVV must be between 3-4 digits" |

### Class-Level Validation

**Custom Validator**: `@FutureExpiryDate`

**Purpose**: Validates that the card expiry date (combining month + year) is in the future. This is a **cross-field validation** that cannot be achieved with field-level annotations alone.

## Testing Strategy

### 1. Service Unit Tests (`PaymentGatewayServiceTest`)

**Purpose**: Test service layer logic in isolation

**Approach**: 
- Mock dependencies (`BankClient`, `PaymentsRepository`)
- Use Mockito to control mock behavior
- Verify business logic and error handling

**Test Coverage**:
- Payment retrieval (success and not found)
- Payment authorization flow
- Payment decline flow
- Bank 5xx error handling (throws exception, no storage)
- Unexpected exception handling (throws exception, no storage)


### 2. API Validation Tests (`PaymentGatewayControllerValidationTest`)

**Purpose**: Test all validation rules comprehensively. Avoid calling bank service with invalid input.

**Approach**:
- Send HTTP requests via MockMvc
- Verify validation error responses

**Test Coverage** (14 tests):
- All fields required (null checks)
- Card number format (14-19 digits, numeric only)
- Expiry month range (1-12)
- Expiry year minimum (2026+)
- Expiry date in future (class-level validation)
- Currency whitelist (GBP, USD, CNY)
- Amount positive value
- CVV format (100-9999)
- Valid request passes all validations

### 3. Integration Tests (`PaymentGatewayControllerTest`)

**Purpose**: Test complete request-to-response flow with real bank service

**Approach**:
- Real bank simulator running in Docker
- Tests actual HTTP communication

**Test Coverage** (5 tests):
- GET payment by ID (success)
- GET payment by ID (not found - 404)
- POST payment - bank authorizes (200 with AUTHORIZED)
- POST payment - bank declines (200 with DECLINED)
- POST payment - bank error (503 Service Unavailable)
