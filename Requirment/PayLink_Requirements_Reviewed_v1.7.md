# PayLink - Backend System Requirements Specification

**Project Name:** PayLink  
**System Type:** Digital Wallet and Utility Payment Backend  
**Version:** 1.7  
**Status:** Assignment-Aligned Requirements Draft

---

## 1. Problem Summary

Customers often face difficulties paying electricity, water, mobile, and internet bills because different service providers use separate payment systems, communication protocols, and confirmation processes.

PayLink is a centralized backend payment system that allows customers to pay utility bills and recharge mobile accounts using its own integrated digital wallet, similar to services such as Fawry and Vodafone Cash.

Customers can create wallets, add money using bank cards, bank transfers, or authorized cash agents, check their balances, and make payments to four supported billers: **ElectroCO, AquaCo, TopUpNet, and NetCo**.

Each biller has different technical requirements. ElectroCO provides fast REST-based electricity billing services, AquaCo uses a slower SOAP-style XML interface, TopUpNet processes mobile top-ups asynchronously, and NetCo processes internet bill payments through daily CSV batches.

PayLink must handle these different integration methods while maintaining accurate wallet balances, preventing duplicate payments, and managing mandatory `Unknown`, `Completed`, `Failed`, and `Refunded` payment outcomes, with strict wallet holds and ledger consistency.

The system also provides configurable fees, customer verification limits, agent commissions, fraud/velocity checks, audit logs, SMS/push notifications with retry, nightly reconciliation, payment receipts, transaction history, refunds, customer support, and financial reporting.

The main objective is to provide a secure, reliable, and convenient digital wallet platform that integrates different billing systems while ensuring accurate financial processing.

---

## 2. Stakeholders

| ID | Stakeholder | Role |
|---|---|---|
| ST-01 | Customer | Creates a wallet, adds funds, and pays ElectroCO, AquaCo, TopUpNet, and NetCo. |
| ST-02 | Agent | Accepts cash deposits and funds customer wallets through authorized transactions. |
| ST-03 | Admin | Manages wallets, agents, billers, service fees, and system configurations. |
| ST-04 | Support Employee | Handles customer complaints, failed payments, and refund requests. |
| ST-05 | Biller Service Providers | ElectroCO, AquaCo, TopUpNet, and NetCo process their respective bill payments or mobile top-ups. |
| ST-06 | Funding Service Provider | Banks and payment gateways facilitate card and bank transfer deposits. |
| ST-07 | Finance and Accounting Team | Monitors wallet transactions, bill payments, agent settlements, and reconciliation. |
| ST-08 | Security and Compliance Officer | Ensures transaction security, fraud prevention, and regulatory compliance. |
| ST-09 | Development and Operations Team | Develops, maintains, and monitors the backend and biller integrations. |

---

## 3. Supported Billers and Integration Requirements

| Biller | Service | Protocol and Format | Required Behavior |
|---|---|---|---|
| **ElectroCO** | Electricity bills | REST, JSON | Fast responses under 1 second. Inquiry by meter number. Payment confirmed immediately when responsive; if it takes over 10 seconds, the canonical status becomes `Unknown` with funds still held. |
| **AquaCo** | Water bills | SOAP-style XML over HTTP | Responses take 2-4 seconds. Different field names and error codes. Rejects payments above the bill amount. |
| **TopUpNet** | Mobile top-up | REST, JSON, asynchronous | Returns `accepted` first (not final), then calls PayLink's callback URL minutes later with `success` or `failed`; duplicate callbacks must be ignored and logged. |
| **NetCo** | Internet bills | Daily batch CSV file | No live inquiry. PayLink uploads one payment file per day and receives a result file the following morning; missing results remain unresolved and held. |

### 3.1 Integration Rules

- **ElectroCO:** PayLink must use the meter number for bill inquiries and process immediate payment confirmations.
- **AquaCo:** PayLink must transform internal requests into SOAP-style XML, handle its specific field names and error codes, and prevent payments above the outstanding bill amount.
- **TopUpNet:** PayLink must distinguish an initial `accepted` response from final success, enforce the 10-second live-biller timeout, and validate/deduplicate asynchronous callbacks.
- **NetCo:** PayLink must queue payments for the daily CSV file, upload the file once per day using stable batch IDs, and process the next-morning result file before finalizing payments; ambiguous uploads or missing results require reconciliation.

---

## 4. Functional Requirements

The 30 existing local FR identifiers and their user stories are retained. **Brief FR-1 through FR-28** refer to the assignment's separately numbered requirements; they are mapped to local FRs in Section 10. All payments must also satisfy the exact state transition rules in Section 7 and configured financial rules in Section 4.6. A scenario tagged **HC-01 to HC-08** is an explicit test obligation, not an invented additional hard case.

### 4.1 Customer and Wallet Management

#### FR-01: Customer Registration and Wallet Creation

**Priority:** Must  
**User Story ID:** US-01

**Requirement:** The system shall allow customers to register and automatically create a PayLink digital wallet.

**User Story:** As a customer, I want to register and receive a wallet so that I can pay utility bills.

**Acceptance Criteria:**

- **Given** a new customer provides a valid mobile number and successfully verifies an OTP,
- **When** registration is completed,
- **Then** the system shall create a customer account and a unique wallet with an initial balance of zero.

**AC-01-B — Basic verification level.**
- **Given** a newly registered customer has a unique account and zero-balance wallet,
- **When** account activation succeeds,
- **Then** the customer shall be assigned the **basic** verification level and its configured daily limit of **5,000 EGP**; a duplicate registration shall not create another wallet.

#### FR-02: Customer Authentication and Payment PIN

**Priority:** Must  
**User Story ID:** US-02

**Requirement:** The system shall securely authenticate customers and allow them to manage payment PINs after identity verification.

**User Story:** As a customer, I want secure login and payment authorization so that my wallet remains protected.

**Acceptance Criteria:**

- **Given** a registered customer provides valid credentials,
- **When** the customer logs in,
- **Then** the system shall authenticate the customer and provide access to protected wallet services.

- **Given** an authenticated customer has completed identity verification,
- **When** the customer sets or resets a valid payment PIN,
- **Then** the system shall securely store the PIN verifier for future payment authorization.

**AC-02-C — Verification upgrade (brief FR-2, Should).**
- **Given** a basic customer has passed the configured identity verification checks,
- **When** an authorized verification upgrade is approved,
- **Then** PayLink shall change the verification level to **verified** and enforce the **30,000 EGP** daily limit for subsequent payments; a failed verification leaves the level unchanged.

#### FR-03: View Wallet Balance

**Priority:** Must  
**User Story ID:** US-03

**Requirement:** The system shall allow customers to check their wallet balances.

**User Story:** As a customer, I want to view my available balance so that I know whether I have enough money to pay.

**Acceptance Criteria:**

- **Given** an authenticated customer owns an active wallet,
- **When** the customer requests wallet information,
- **Then** the system shall return the total balance, reserved balance, and available balance.

**AC-03-B — Available versus held balance.**
- **Given** a wallet has 500.00 EGP posted balance and an active 125.00 EGP hold,
- **When** the owner requests the balance,
- **Then** the response shall show **375.00 EGP available** and **125.00 EGP held**, reconciled to the immutable wallet ledger.

#### FR-04: Fund Wallet Using Bank Cards

**Priority:** Must  
**User Story ID:** US-04

**Requirement:** The system shall allow customers to fund their wallets using supported debit or credit cards.

**User Story:** As a customer, I want to add money using my bank card so that I can fund my wallet electronically.

**Acceptance Criteria:**

- **Given** a customer selects bank card funding and enters a valid amount,
- **When** the customer completes payment gateway authorization,
- **Then** the system shall record a funding transaction and await verified payment confirmation before crediting the wallet.

**AC-04-B — Card confirmation and replay safety.**
- **Given** a card top-up is awaiting its gateway confirmation,
- **When** PayLink receives a verified payment-success webhook twice,
- **Then** exactly one ledger credit shall be posted and the duplicate notification shall not change the balance.

#### FR-05: Fund Wallet Using Bank Transfers

**Priority:** Must  
**User Story ID:** US-05

**Requirement:** The system shall support wallet funding through bank transfers.

**User Story:** As a customer, I want to fund my wallet through a bank transfer so that I can use money from my bank account.

**Acceptance Criteria:**

- **Given** a customer selects bank transfer funding and enters a valid amount,
- **When** the customer initiates the deposit,
- **Then** the system shall generate a transfer reference and keep the transaction Pending until the received funds are verified.

**AC-05-B — Verified bank settlement.**
- **Given** a bank-transfer request has a unique reference but no matching bank receipt,
- **When** the customer reports that the transfer was sent,
- **Then** the request shall remain uncredited until a verified bank receipt matches the reference, amount, and intended wallet.

#### FR-06: Agent-Assisted Wallet Funding

**Priority:** Must  
**User Story ID:** US-06

**Requirement:** The system shall allow authorized agents to deposit customer cash through confirmed, settlement-backed funding controls and to initiate customer-authorized bill payments (commission on completion).

**User Story:** As an agent, I want to accept customer cash deposits so that customers can fund their PayLink wallets.

**Acceptance Criteria:**

- **Given** an authorized agent receives cash and identifies a valid customer wallet,
- **When** the agent submits a deposit within the approved funding allowance,
- **Then** the system shall create a unique cash funding transaction for confirmation.

**AC-06-B — Agent identity and cash confirmation.**
- **Given** an agent is suspended, unauthenticated, has insufficient settlement-backed allowance, or has not confirmed receipt of cash,
- **When** that agent attempts to confirm a customer cash deposit,
- **Then** PayLink shall reject confirmation, leave the customer wallet unchanged, and audit the attempted operation.

**AC-06-C — Successful agent cash deposit.**
- **Given** an active authenticated agent has received the cash, passed wallet/account checks, and has sufficient approved funding allowance,
- **When** the agent explicitly confirms receipt with a unique deposit reference,
- **Then** PayLink shall atomically consume that allowance and credit the customer's wallet **once**, with a ledger entry containing the agent ID, amount, and confirmation timestamp.

**AC-06-D — Agent-assisted bill payment and commission (brief FR-17, Should).**
- **Given** an authorized agent initiates a customer's bill payment with the customer's required authorization,
- **When** the biller confirms the payment and its state becomes `Completed`,
- **Then** PayLink shall record and credit the agent's commission as **40% of the earned service fee exactly once**; no commission is credited for `Rejected`, `Unknown`, `Failed`, or `Refunded` payments.

#### FR-07: Confirm or Reject Wallet Funding

**Priority:** Must  
**User Story ID:** US-07

**Requirement:** The system shall credit wallets only after successful funding verification and reject definitively failed funding transactions.

**User Story:** As a customer, I want my balance updated correctly after deposits so that it reflects my actual funds.

**Acceptance Criteria:**

- **Given** a Pending funding transaction exists,
- **When** the backend verifies successful funding,
- **Then** the system shall credit the wallet exactly once and mark the transaction as Successful.

- **Given** a Pending funding transaction exists,
- **When** a definitive funding failure is confirmed,
- **Then** the system shall mark it as Failed without crediting the wallet.

**AC-07-C — Same deposit result delivered concurrently.**
- **Given** two verified confirmations for the same funding transaction arrive concurrently,
- **When** PayLink applies the confirmations,
- **Then** one immutable ledger credit shall be committed and the second confirmation shall resolve to the existing result without a new credit.

#### FR-08: Wallet Transaction History

**Priority:** Must  
**User Story ID:** US-08

**Requirement:** The system shall maintain an immutable exact-decimal internal ledger for all wallet money movements and allow customers to view their transactions.

**User Story:** As a customer, I want to view my deposits and payments so that I can track my money.

**Acceptance Criteria:**

- **Given** an authenticated customer has wallet transactions,
- **When** the customer requests wallet history,
- **Then** the system shall return the customer's recorded deposits, deductions, and refunds, including their amounts, dates, and statuses.

**AC-08-B — Ledger-only financial mutations (brief FR-5, Must).**
- **Given** an authorized deposit, hold, release, final charge, fee allocation, or refund changes money,
- **When** that operation commits,
- **Then** PayLink shall append traceable **exact-decimal** ledger entries, recompute or reconcile available/held balances from the ledger, and never directly overwrite a stored balance without a corresponding ledger operation.

#### FR-09: Manage Wallet Restrictions

**Priority:** Must  
**User Story ID:** US-09

**Requirement:** The system shall allow authorized administrators and duly permitted support staff to freeze or unfreeze wallets, while all concurrent wallet reservations remain atomic.

**User Story:** As an admin, I want to restrict suspicious wallets so that unauthorized payments can be prevented.

**Acceptance Criteria:**

- **Given** an authorized admin selects an active wallet,
- **When** the admin freezes the wallet,
- **Then** the system shall block new outgoing payments while preserving the existing balance and transaction history.

**AC-09-B — Concurrent wallet operations (mandatory HC-04).**
- **Given** one wallet has available funds to cover **only one** of two different valid payment requests arriving simultaneously,
- **When** both requests pass through atomic funds validation and reservation,
- **Then** exactly one request shall transition `Initiated -> FundsHeld`, the other shall transition `Initiated -> Rejected`, only one hold shall exist, and **available balance shall never be negative**.

**AC-09-C — Authorized support control (brief FR-21, Should).**
- **Given** a support employee has account-restriction permission and an auditable case reference,
- **When** the employee blocks or unblocks a customer account,
- **Then** the new restriction shall apply to subsequent payment requests and the actor, reason, and timestamp shall be logged.

### 4.2 Biller Management and Inquiry

#### FR-10: View Supported Billers

**Priority:** Must  
**User Story ID:** US-10

**Requirement:** The system shall provide a list of the four supported billers and their available services.

**User Story:** As a customer, I want to view supported billers so that I can select the service I need.

**Acceptance Criteria:**

- **Given** an authenticated customer accesses the bill payment service,
- **When** the customer requests the list of active billers,
- **Then** the system shall return ElectroCO, AquaCo, TopUpNet, and NetCo when their services are enabled.

**AC-10-B — Required biller fields (brief FR-7, Must).**
- **Given** an active biller has configured account field rules and minimum/maximum accepted amounts,
- **When** the customer opens its catalog details,
- **Then** PayLink shall return the required input fields, validation rules, status, and configured amount limits without hard-coding them in the application client.

#### FR-11: Validate Biller Input Information

**Priority:** Must  
**User Story ID:** US-11

**Requirement:** The system shall validate the customer-provided identifiers and input fields required by each biller.

**User Story:** As a customer, I want PayLink to validate my payment information so that I avoid submitting incorrect details.

**Acceptance Criteria:**

- **Given** a customer selects a biller and enters its required information,
- **When** the customer submits the request,
- **Then** the system shall validate the input against that biller's configured format and reject invalid information before submitting the request.

**AC-11-B — Biller-specific validation.**
- **Given** the user sends a missing ElectroCO meter number, malformed TopUpNet mobile number, or AquaCo provider-specific invalid field,
- **When** PayLink validates the request,
- **Then** it shall return a deterministic field-level validation error without making a biller payment call or creating a wallet hold.

#### FR-12: ElectroCO Electricity Bill Inquiry

**Priority:** Must  
**User Story ID:** US-12

**Requirement:** The system shall retrieve ElectroCO electricity bills through its REST/JSON API using a meter number.

**User Story:** As a customer, I want to retrieve my ElectroCO bill using my meter number so that I know how much electricity payment is due.

**Acceptance Criteria:**

- **Given** a customer selects ElectroCO and enters a valid meter number,
- **When** the customer requests a bill inquiry,
- **Then** the system shall send a REST request and return the JSON billing details received from ElectroCO.

- **Given** ElectroCO responds within its expected time of under 1 second,
- **When** PayLink receives the response,
- **Then** the system shall make the returned bill information available to the customer without unnecessary processing delay.

**AC-12-C — Canonical quote.**
- **Given** ElectroCO returns a due amount of **350.00 EGP** for a valid meter,
- **When** PayLink displays the electricity quotation outside a promotion,
- **Then** it shall display **350.00 EGP bill + 2.50 EGP fee = 352.50 EGP total**, all using exact decimal arithmetic.

#### FR-13: AquaCo Water Bill Inquiry

**Priority:** Must  
**User Story ID:** US-13

**Requirement:** The system shall retrieve AquaCo water bills through SOAP-style XML over HTTP and map provider-specific fields and error codes.

**User Story:** As a customer, I want to retrieve my AquaCo water bill so that I can review the amount due.

**Acceptance Criteria:**

- **Given** a customer selects AquaCo and provides a valid water billing identifier,
- **When** the customer requests a bill inquiry,
- **Then** the system shall construct and send the required SOAP-style XML request and convert the response into PayLink's standard bill format.

- **Given** AquaCo returns an error code or responds after 2-4 seconds,
- **When** PayLink processes the response,
- **Then** the system shall correctly map any returned error and handle the expected response delay.

**AC-13-C — SOAP contract isolation.**
- **Given** AquaCo returns a known provider-specific error element in an XML response after 4 seconds,
- **When** the AquaCo adapter processes the response,
- **Then** it shall translate the error to PayLink's stable internal error format without exposing XML field names to other payment modules.

#### FR-14: TopUpNet Mobile Recharge Validation

**Priority:** Must  
**User Story ID:** US-14

**Requirement:** The system shall accept mobile top-up requests containing a valid mobile number and recharge amount.

**User Story:** As a customer, I want to enter my mobile number and recharge amount so that I can top up my mobile balance.

**Acceptance Criteria:**

- **Given** a customer selects TopUpNet,
- **When** the customer provides a valid mobile number and an amount that satisfies the configured recharge rules,
- **Then** the system shall accept the recharge details for payment authorization without requiring an outstanding bill inquiry.

**AC-14-B — Mobile top-up fee.**
- **Given** a customer requests a **50.00 EGP** TopUpNet top-up outside a promotion,
- **When** PayLink quotes the top-up,
- **Then** it shall charge the **1.00 EGP minimum** fee (greater than 1.5% of 50.00 EGP) and display **51.00 EGP total**.

#### FR-15: NetCo Internet Payment Preparation

**Priority:** Should  
**User Story ID:** US-15

**Requirement:** The system shall accept NetCo internet payment information without attempting a live provider inquiry.

**User Story:** As a customer, I want to submit my NetCo account reference and payment amount so that my internet bill can be paid through the daily batch process.

**Acceptance Criteria:**

- **Given** a customer selects NetCo and provides a valid account reference and payment amount,
- **When** the customer requests an internet payment,
- **Then** the system shall validate the required input format and prepare the payment for batch processing without making a live inquiry request to NetCo.

**AC-15-B — No fictional NetCo inquiry.**
- **Given** NetCo provides only CSV batch payments and has no live inquiry endpoint,
- **When** the customer requests a payment quotation,
- **Then** PayLink shall clearly label the amount as **customer-provided/unverified by NetCo**, apply the internet fee rule, and **not claim** that the biller has confirmed the amount.

### 4.3 Payment Processing and Biller Integration

#### FR-16: Authorize Payments and Validate Wallet Funds

**Priority:** Must  
**User Story ID:** US-16

**Requirement:** The system shall validate the payment PIN, available wallet balance, verification-level payment limits, velocity rules, and account blocks before processing payments.

**User Story:** As a customer, I want secure payment authorization so that I cannot make unauthorized or unaffordable payments.

**Acceptance Criteria:**

- **Given** a customer submits a payment request,
- **When** the payment PIN is incorrect, the wallet balance is insufficient, or a configured transaction limit is exceeded,
- **Then** the system shall reject the request without deducting or reserving wallet funds.

**AC-16-B — Configured monetary limits (brief FR-18, Must).**
- **Given** the customer is basic (daily limit **5,000 EGP**) or verified (daily limit **30,000 EGP**), and a single bill payment must be between **5 and 10,000 EGP**, inclusive,
- **When** a payment breaches the configured single-payment range or would exceed the customer's accumulated daily payment limit,
- **Then** PayLink shall transition `Initiated -> Rejected`, create **no hold**, and return the specific limit and reason.

**AC-16-C — Velocity check (brief FR-19, Should).**
- **Given** one account has already made **five** payment attempts in the last **60 seconds**,
- **When** a sixth attempt arrives in the same rolling window,
- **Then** PayLink shall reject that attempt, block further attempts for **10 minutes**, and record the fraud-rule decision; blocked accounts cannot initiate bill payments.

#### FR-17: Create Payment Transactions and Reserve Funds

**Priority:** Must  
**User Story ID:** US-17

**Requirement:** The system shall create a unique payment transaction and atomically reserve the required wallet funds.

**User Story:** As a customer, I want my funds safely reserved so that they cannot be used for another payment while processing.

**Acceptance Criteria:**

- **Given** a payment has been authorized and the customer has sufficient available funds,
- **When** the backend initiates processing,
- **Then** the system shall create a unique transaction ID, reserve the required amount, and record the `Initiated -> FundsHeld` transition once the hold commits; no `Processing` canonical state is created.

**AC-17-B — Atomic state and hold.**
- **Given** a payment is in `Initiated` and fraud checks, limits, PIN, and available-funds checks pass,
- **When** the wallet hold is atomically committed,
- **Then** PayLink shall transition **`Initiated -> FundsHeld`**, reserve the full quote (bill plus fee), and append a ledger hold entry; an authorization failure instead transitions **`Initiated -> Rejected`** with no hold.

**AC-17-C — Dispatch boundary.**
- **Given** an authorized payment is in `FundsHeld`,
- **When** the biller adapter submits the live request or a NetCo batch containing that payment is confirmed as sent,
- **Then** PayLink shall transition **`FundsHeld -> SentToBiller`** once, preserving the hold until a final result is confirmed.

#### FR-18: Prevent Duplicate Transactions

**Priority:** Must  
**User Story ID:** US-18

**Requirement:** The system shall prevent duplicate bill payments and wallet deductions using transaction references and idempotency controls.

**User Story:** As a customer, I want duplicate payments prevented so that I am not charged twice.

**Acceptance Criteria:**

- **Given** an identical payment request has already been accepted using the same idempotency key, or a uniquely identified bill has an active or successful payment,
- **When** the request is submitted again,
- **Then** the system shall return the existing transaction or reject the duplicate without creating another wallet debit.

**AC-18-B — Same-key retry (mandatory HC-01).**
- **Given** a mobile app sent a payment request with idempotency key `K-001`, producing transaction `T-001`, and the network response was lost,
- **When** it repeats the **same payload and key** `K-001`,
- **Then** PayLink shall return the **original transaction ID, current state and original result representation**, create **no second payment/hold/debit**, and record the retry.

**AC-18-C — Key reused with different payload.**
- **Given** idempotency key `K-001` is already bound to one canonical payment payload,
- **When** a new request reuses `K-001` with a different amount, biller, or account reference,
- **Then** PayLink shall reject it with an idempotency-conflict error and perform **no wallet mutation**.

#### FR-19: Process ElectroCO Electricity Payments

**Priority:** Must  
**User Story ID:** US-19

**Requirement:** The system shall submit ElectroCO electricity payments through REST/JSON and process immediate payment confirmation.

**User Story:** As a customer, I want my ElectroCO electricity bill paid and confirmed immediately so that I know the payment result.

**Acceptance Criteria:**

- **Given** a customer has authorized a valid ElectroCO electricity payment with reserved funds,
- **When** PayLink submits the payment through ElectroCO's REST API,
- **Then** the system shall process ElectroCO's immediate confirmation and finalize the wallet deduction only when payment success is confirmed.

**AC-19-B — Slow ElectroCO response (mandatory HC-02).**
- **Given** an ElectroCO payment is in `SentToBiller` with funds held and ElectroCO takes **12 seconds** to respond,
- **When** the mandatory **10-second live-biller timeout** expires without a definitive result,
- **Then** PayLink shall transition **`SentToBiller -> Unknown`**, keep the full hold, and show the customer **'being processed'**; it shall **not refund or resend the charge** based on timeout alone.

**AC-19-C — Late reply is reconciled.**
- **Given** the ElectroCO payment is `Unknown` and still held,
- **When** ElectroCO subsequently confirms the original transaction using its unique reference,
- **Then** PayLink shall transition **`Unknown -> Completed`** and finalize the ledger debit exactly once.

#### FR-20: Process AquaCo Water Payments

**Priority:** Must  
**User Story ID:** US-20

**Requirement:** The system shall submit AquaCo water bill payments through SOAP-style XML and correctly handle delayed responses, field mappings, error codes, and overpayment rejection.

**User Story:** As a customer, I want my AquaCo bill processed correctly so that I do not lose money when the provider rejects a payment.

**Acceptance Criteria:**

- **Given** a customer authorizes a valid AquaCo payment not exceeding the confirmed bill amount,
- **When** PayLink sends the SOAP-style XML payment request,
- **Then** the system shall handle the expected 2-4 second response, interpret the provider's confirmation, and finalize the payment only upon confirmed success.

- **Given** AquaCo definitively rejects a payment because its amount exceeds the bill amount or because of a provider-specific validation error,
- **When** PayLink receives the rejection,
- **Then** the system shall map the error, transition the payment `SentToBiller -> Failed` and, upon confirmed hold release, `Failed -> Refunded`.

**AC-20-C — AquaCo provider refusal (mandatory HC-03).**
- **Given** a valid AquaCo payment is `SentToBiller` with an active hold,
- **When** AquaCo returns a documented refusal/error code,
- **Then** PayLink shall translate the provider error into a clear reason, transition **`SentToBiller -> Failed`**, release the hold by transitioning **`Failed -> Refunded`**, and enqueue an **SMS** explaining the failure.

#### FR-21: Submit TopUpNet Asynchronous Payments

**Priority:** Must  
**User Story ID:** US-21

**Requirement:** The system shall initiate TopUpNet mobile recharge requests through REST/JSON and treat the initial `accepted` response as a non-final status.

**User Story:** As a customer, I want to submit a mobile recharge and track its processing so that I know whether it is still waiting for confirmation.

**Acceptance Criteria:**

- **Given** a customer authorizes a valid TopUpNet recharge,
- **When** TopUpNet responds with `accepted`,
- **Then** the system shall keep the canonical state `SentToBiller`, retain the hold, and await the final callback; after the 10-second live-biller timeout without a definitive outcome it shall transition to `Unknown`, never to `Completed` based only on `accepted`.

**AC-21-B — Acknowledgment is not confirmation.**
- **Given** a TopUpNet recharge was submitted in state `SentToBiller`,
- **When** TopUpNet returns only `accepted` without a final result,
- **Then** PayLink shall retain the hold and **not** transition to `Completed`; at the **10-second live-biller timeout** without final confirmation it shall move **`SentToBiller -> Unknown`** and await the callback.

#### FR-22: Process TopUpNet Payment Callbacks

**Priority:** Must  
**User Story ID:** US-22

**Requirement:** The system shall receive and validate asynchronous TopUpNet callbacks containing final recharge results.

**User Story:** As a customer, I want PayLink to update my recharge status when TopUpNet responds so that I know whether my top-up succeeded.

**Acceptance Criteria:**

- **Given** a TopUpNet transaction is `SentToBiller` or `Unknown`,
- **When** PayLink receives an authenticated callback containing `success`,
- **Then** the system shall transition the recharge to `Completed` and finalize the wallet deduction exactly once.

- **Given** a TopUpNet transaction is `SentToBiller` or `Unknown`,
- **When** PayLink receives an authenticated callback containing `failed`,
- **Then** the system shall transition to `Failed`, then release the hold and transition `Failed -> Refunded` exactly once.

**AC-22-C — Duplicate TopUpNet callback (mandatory HC-06).**
- **Given** a TopUpNet transaction has already transitioned **`Unknown -> Completed`** and has exactly one finalized debit,
- **When** TopUpNet sends a **second authenticated `success` callback** for the same provider transaction reference,
- **Then** PayLink shall ignore it for state/ledger mutation, return an acknowledgment to the sender, and **log the duplicate callback** with its correlation ID.

**AC-22-D — Invalid or contradictory callbacks.**
- **Given** a callback has an invalid signature, unknown payment reference, or conflicts with a final state,
- **When** PayLink receives it,
- **Then** the callback shall not change payment state or ledger and shall be rejected or quarantined and audited for investigation.

#### FR-23: Generate and Upload NetCo Daily Payment Batch

**Priority:** Should  
**User Story ID:** US-23

**Requirement:** The system shall collect valid NetCo payment requests and generate one CSV payment batch for upload each day, with replay-safe batch identifiers and upload-failure handling.

**User Story:** As a customer, I want my NetCo internet payment included in the daily payment batch so that my bill can be processed.

**Acceptance Criteria:**

- **Given** authorized NetCo payments have been queued with reserved funds,
- **When** the scheduled daily batch execution occurs,
- **Then** the system shall generate a CSV file containing the required payment references, account identifiers, and amounts, upload it to NetCo through the configured file exchange mechanism, and record its submission status.

**AC-23-B — Batch upload failure.**
- **Given** NetCo transactions are `FundsHeld` and flagged internally as queued,
- **When** CSV generation fails or a batch is **confirmed not received** by NetCo,
- **Then** the payments shall remain `FundsHeld` (not `Completed` or `Failed`), their holds shall remain, and the batch shall be retried safely using its original batch/payment references.

**AC-23-C — Uncertain upload outcome.**
- **Given** the upload connection fails after transmission and receipt is uncertain,
- **When** PayLink cannot establish whether NetCo received the CSV,
- **Then** it shall reconcile by the stable batch ID before resending to avoid duplicate provider payments.

#### FR-24: Process NetCo Next-Morning Result File

**Priority:** Should  
**User Story ID:** US-24

**Requirement:** The system shall receive and process NetCo's next-morning CSV result file to determine the final outcome of submitted payments.

**User Story:** As a customer, I want my NetCo payment status updated after batch processing so that I know whether my internet bill was paid.

**Acceptance Criteria:**

- **Given** NetCo payments were submitted in the previous daily CSV batch,
- **When** PayLink receives the next-morning result file,
- **Then** the system shall match result records to their original transactions and apply only the valid `SentToBiller/Unknown -> Completed` or `SentToBiller/Unknown -> Failed -> Refunded` transitions appropriate to the confirmed result.

- **Given** an expected NetCo result is missing or cannot be safely matched,
- **When** the result file is processed,
- **Then** the system shall keep the hold, flag it for reconciliation, and transition `SentToBiller -> Unknown` only when the configured batch-result deadline has passed; never assume success or failure.

**AC-24-C — NetCo definitive results.**
- **Given** a NetCo record is in `SentToBiller` or `Unknown` with funds held,
- **When** a validated next-morning result file reports **success** or **refusal** for its unique payment reference,
- **Then** PayLink shall transition to **`Completed`** on success, or first to **`Failed`** then to **`Refunded`** after releasing the hold on refusal, without duplicate financial entries.

**AC-24-D — Missing or invalid result file.**
- **Given** the expected result file is absent, corrupted, or contains unmatched rows at the configured **next-morning result deadline**,
- **When** reconciliation cannot establish a definitive outcome,
- **Then** affected payments already in `SentToBiller` shall transition **`SentToBiller -> Unknown`**, remain held, and be reported as unresolved; no automatic refund is allowed.

#### FR-25: Finalize Successful and Failed Bill Payments

**Priority:** Must  
**User Story ID:** US-25

**Requirement:** The system shall finalize wallet deductions only after confirmed success and release reservations after confirmed failure.

**User Story:** As a customer, I want my wallet balance updated accurately so that I am charged only for successful payments.

**Acceptance Criteria:**

- **Given** a payment receives a verified success result from ElectroCO, AquaCo, TopUpNet, or NetCo,
- **When** PayLink processes the result,
- **Then** the system shall finalize the reserved amount as a wallet debit exactly once and transition the transaction to `Completed`.

- **Given** a payment receives a definitive failure result,
- **When** PayLink processes the failure,
- **Then** the system shall transition the transaction to `Failed`, release the reserved funds, and then transition `Failed -> Refunded`.

**AC-25-C — Fee and commission posting on completion.**
- **Given** a biller definitively confirms an agent-assisted payment whose fee and agent ID were fixed at authorization,
- **When** the transaction transitions **`SentToBiller/Unknown -> Completed`**,
- **Then** PayLink shall atomically convert the full hold into the final charge, credit PayLink's fee income, and credit the agent wallet with **40% of the service fee** exactly once using exact-decimal ledger entries.

**AC-25-D — No direct Completed refund transition.**
- **Given** an original payment is `Completed` (a mandatory final state),
- **When** a later approved customer refund is necessary,
- **Then** the original payment shall **remain `Completed`** and a separately referenced compensating refund transaction shall credit the wallet; a failed payment uses **`Failed -> Refunded`** after a hold release.

#### FR-26: Manage Pending Payments and Reconciliation

**Priority:** Must  
**User Story ID:** US-26

**Requirement:** The system shall track `Unknown` and unresolved batch payments, re-query live billers at the required intervals, and reconcile settlement discrepancies without creating duplicate transactions.

**User Story:** As a finance employee, I want pending transactions monitored so that wallet balances remain accurate even when billers respond late.

**Acceptance Criteria:**

- **Given** a payment is awaiting a TopUpNet callback, a NetCo result file, or a definitive response from another biller,
- **When** the expected confirmation is delayed or unavailable,
- **Then** the system shall preserve the hold, use `Unknown` only after the applicable timeout/deadline, and flag or schedule the payment for reconciliation without making another payment.

**AC-26-B — Mandatory Unknown retry timing.**
- **Given** a live-biller payment entered `Unknown` because no definitive response arrived within 10 seconds,
- **When** **1, 5, and 15 minutes** have elapsed since entering `Unknown`,
- **Then** PayLink shall **re-query the original biller transaction/reference** at most once at each scheduled time (three queries total), with no new wallet debit or new payment submission, and shall await reconciliation if still unresolved.

**AC-26-C — Settlement conflict (mandatory HC-07).**
- **Given** a nightly settlement file lists a biller payment whose PayLink record is `Failed` (or `Refunded` after a failure),
- **When** reconciliation compares the settlement file with PayLink's ledger and payment states,
- **Then** PayLink shall create a **high-priority discrepancy** in the reconciliation report linked to the reference for support review, without silently changing a final state or automatically charging the wallet again.

### 4.4 Transaction Tracking and Customer Support

#### FR-27: Payment History, Receipts, and Notifications

**Priority:** Must  
**User Story ID:** US-27

**Requirement:** The system shall allow customers to view payment statuses and histories, obtain receipts for successful payments, and receive SMS/push notifications with independent retries and delivery tracking.

**User Story:** As a customer, I want to track my payments and receive receipts so that I have proof of completed transactions.

**Acceptance Criteria:**

- **Given** an authenticated customer has previous bill payments,
- **When** the customer requests payment history,
- **Then** the system shall return customer-owned payment records with their current statuses.

- **Given** a transaction is confirmed `Completed`,
- **When** the customer requests its receipt,
- **Then** the system shall return the transaction ID, biller, amount, fees, and completion date.

- **Given** a transaction becomes `Unknown`, `Completed`, or `Failed`/`Refunded`,
- **When** the backend records that status change,
- **Then** it shall generate a notification event for the customer.

**AC-27-D — SMS provider unavailable (mandatory HC-05).**
- **Given** a payment has definitively reached `Completed` and the SMS provider is down,
- **When** PayLink attempts delivery,
- **Then** payment completion and the final ledger debit shall remain successful and the SMS shall be **retried three times**; if still unsuccessful it shall be marked **undelivered** and logged, without reversing the payment.

**AC-27-E — Required completion/failure channels.**
- **Given** a payment becomes `Completed` or `Failed`/`Refunded` following refusal,
- **When** its state change is committed,
- **Then** PayLink shall enqueue **SMS and push** notifications (and optional email) with the final outcome; failed notifications shall be retried independently of the payment state.

#### FR-28: Manage Support Tickets and Refunds

**Priority:** Must  
**User Story ID:** US-28

**Requirement:** The system shall allow customers to report transaction issues and authorized support employees to manage complaints and initiate eligible refunds through approved controls.

**User Story:** As a support employee, I want to investigate payment issues and request refunds so that customer complaints can be resolved.

**Acceptance Criteria:**

- **Given** a customer reports a problem with an existing transaction,
- **When** a support ticket is submitted,
- **Then** the system shall create a unique ticket linked to that transaction.

- **Given** an authorized refund process confirms that a previously deducted payment is eligible for reversal,
- **When** the approved refund is executed,
- **Then** the system shall credit the eligible amount exactly once and record the refund against the original transaction.

**AC-28-C — Support full reference lookup (brief FR-20, Must).**
- **Given** a support employee has payment-investigation permission and a valid payment reference,
- **When** the employee searches for that reference,
- **Then** PayLink shall return the payment's full chronological state/ledger/callback/notification history, masked sensitive fields, and associated disputes, recording the access in the audit log.

**AC-28-D — Completed-payment refund approval (brief FR-27, Could).**
- **Given** a customer requests a refund for an original `Completed` payment and support verifies eligibility with a traceable approval,
- **When** an authorized approver accepts the request,
- **Then** PayLink shall post a separate idempotent **compensating refund entry** linked to the original payment without changing its `Completed` state; a declined request shall create no refund entry.

### 4.5 Administration and Reporting

#### FR-29: Manage Billers and Fees

**Priority:** Must  
**User Story ID:** US-29

**Requirement:** The system shall allow authorized administrators to add, edit, or disable billers and services and manage configurable fees, promotions, and payment limits without redeployment.

**User Story:** As an admin, I want to configure billers and fees so that PayLink operates correctly and charges appropriate amounts.

**Acceptance Criteria:**

- **Given** an authorized administrator updates a valid biller configuration or fee rule,
- **When** the changes are saved,
- **Then** the system shall apply the updated configuration to subsequent eligible transactions without altering historical records.

**AC-29-B — Fee rules and editable limits (brief FR-16 Must; FR-24 Should).**
- **Given** a bill is quoted outside an active promotion,
- **When** PayLink calculates the fee,
- **Then** electricity and water shall each use **2.50 EGP**, mobile shall use **max(1.5% of amount, 1 EGP)**, and internet shall use **2 EGP up to and including 200 EGP, otherwise 4 EGP**; calculations shall use exact-decimal rounding rules.

**AC-29-C — Configurable promotion.**
- **Given** an administrator configures one chosen biller, a promotion start and end time, and fee `0 EGP`,
- **When** a quote is calculated during that active promotion window,
- **Then** the selected biller's fee shall be **0 EGP** and other billers' fee rules shall be unchanged; fee/limit edits shall take effect without redeployment.

**AC-29-D — Extensibility (mandatory HC-08).**
- **Given** a new biller with a different transport and fields must be supported,
- **When** the engineering team adds the biller,
- **Then** only a **new integration adapter and configuration** shall be necessary; existing wallet, payment, fee, and other biller modules shall remain unchanged, verified by module-level regression tests and a changed-files review.

#### FR-30: Generate Financial Reports

**Priority:** Could  
**User Story ID:** US-30

**Requirement:** The system shall allow authorized finance personnel to generate financial reports for wallet deposits, payment transactions, fees, refunds, and agent settlements.

**User Story:** As a finance employee, I want financial reports so that I can monitor financial activity and reconcile transactions.

**Acceptance Criteria:**

- **Given** an authorized finance employee selects a valid reporting period,
- **When** the employee requests a financial report,
- **Then** the system shall return wallet deposits, biller payment totals, fees, refunds, agent settlement records, and transaction statuses for the selected period.

**AC-30-B — Daily accounting and mismatch visibility.**
- **Given** an authorized finance employee selects a day with a completed reconciliation run,
- **When** the daily summary is generated,
- **Then** it shall separate **payments, fees earned, agent commissions paid, refunds, and failures per biller**, include reconciliation discrepancy counts, and link mismatches to support investigations.

### 4.6 Functional Requirement Priority Summary and Fixed Business Rules

| Priority | Count | Rationale |
|---|---:|---|
| **Must** | **26** | Assignment-Must capabilities and their indispensable wallet/payment/security foundations. |
| **Should** | **3** | NetCo batch support is included in this user-selected scope but is **optional in Brief FR-15**. |
| **Could** | **1** | Additional daily management totals; essential reconciliation remains mandatory under local FR-26. |
| **Total** | **30** | Local FR-01 through FR-30, with US-01 through US-30. |

**Mandatory configurable business rules from assignment Section 2.6** (values are stored in configuration, not source code):

| Rule | Required value |
|---|---|
| Currency / numerical representation | EGP, **exact decimal types**, never binary floating point. |
| Daily customer payment limit | Basic: **5,000 EGP**; verified: **30,000 EGP**. |
| Single payment amount | **5–10,000 EGP**, inclusive (bill/top-up principal; total including fees also checked against available funds). |
| Electricity and water fee | **2.50 EGP** fixed. |
| Mobile top-up fee | **1.5%** of principal, minimum **1 EGP**. |
| Internet fee | **2 EGP** for amounts up to **200 EGP**; **4 EGP** above **200 EGP**. |
| Promotion | **0 EGP** service fee for one configured biller during chosen start/end dates. |
| Agent commission | **40%** of earned service fee, credited to agent wallet only on `Completed`. |
| Velocity restriction | More than **5 payments in 60 seconds** blocks account for **10 minutes**. |
| Live biller timeout | **10 seconds** without definitive answer triggers `SentToBiller -> Unknown`. |
| Unknown live-biller re-queries | At **1, 5, and 15 minutes** after entering `Unknown`; **max 3** status re-queries, then settlement reconciliation. |
| Notification failure retry | **3 retries**, then record **undelivered**. |

**Prioritization rule:** Explicit Brief-`Must` requirements are never demoted; local FR-15, FR-23, and FR-24 are prioritized `Should` because the brief makes **NetCo optional**, even though the team requested it in the planned integration scope. Local FR-30 (`Could`) is enhanced reporting, not the mandatory handling of settlement mismatches (local FR-26).

---

## 5. Non-Functional Requirements

The first nine NFR identifiers are preserved from Version 1.6. Three new ones (NFR-10–12) close the assignment's explicit resilience, modifiability, and CI coverage gaps. Test evidence is expected in CI, integration tests, and the load/recovery reports; targets in addition to those specified by the brief are **proposed**.

| ID | Quality / Requirement | Measurable and Testable Acceptance Criteria | Priority |
|---|---|---|---|
| NFR-01 | Security | **100%** of public endpoints use TLS **1.2+**; payment PIN verifiers use a slow one-way password hash; no raw card CVV/full PAN is persisted; automated scans find **zero** known critical vulnerabilities at release (last target proposed). | **Must** |
| NFR-02 | Performance and throughput | Process at least **100 payments/second sustained for 60 seconds** against mock billers, with **zero unintended duplicate debits** (brief target). Separately, at **100 non-provider API requests/second for 30 minutes**, **95%** complete within **800 ms** of PayLink processing time (proposed). | **Must** |
| NFR-03 | Availability | PayLink's core wallet and payment APIs achieve at least **99.9%** measured monthly availability, excluding separately documented upstream biller outages (**proposed** target). | **Must** |
| NFR-04 | Financial correctness and ledger consistency | Across all **8 hard-case automated tests** plus **10,000** concurrent/duplicate payment trials: **zero double charges**, **zero negative available balances**, **zero mismatches** between posted/held balances and the exact-decimal ledger; payment finalization is atomic. | **Must** |
| NFR-05 | Integration contract correctness | **100%** of defined test contract cases for ElectroCO REST/JSON, AquaCo SOAP/XML, TopUpNet callbacks and NetCo CSV are parsed and mapped correctly; unexpected schemas produce controlled errors, not malformed financial writes. | **Must** |
| NFR-06 | Role-based access control | **100%** of protected customer, agent, support, admin and finance endpoints reject requests without the required identity/role in automated security tests; wallet A cannot read wallet B's financial details. | **Must** |
| NFR-07 | Scalability | Under healthy simulated adapters, sustain **500 concurrent authenticated clients** and **100 payment initiation requests/second for 30 minutes**, with no functional failure or data-loss (**proposed**). | **Should** |
| NFR-08 | Recovery | In a documented recovery drill, the application meets **RTO ≤ 60 minutes** and **RPO ≤ 5 minutes**, and restored ledger totals match recoverable entries (**proposed**). | **Should** |
| NFR-09 | Auditability | **100%** of money movements and administrative actions have an immutable trace with **who, when, why, amount, transaction/reference, and outcome**; verify through ledger/audit reconciliation tests. | **Must** |
| NFR-10 | Resilience and fault isolation | In automated fault-injection tests, bringing **one biller adapter fully down** shall not prevent successful payments to the other **three healthy billers**; no unrelated wallet holds shall leak and unavailable-provider operations shall fail or remain `Unknown` as specified. | **Must** |
| NFR-11 | Modifiability / extensibility | Adding a mock **fifth** biller or applying ElectroCO API v2 (renamed fields, EGP-to-piaster conversion, signed requests) shall modify **only the corresponding Biller Integration adapter and biller configuration**, with **zero changes** to Accounts/Wallet, Payment Processing, Fees, or existing adapters; verified using a Git diff and regression suite. | **Must** |
| NFR-12 | Testability and coverage | CI shall report at least **70% line coverage of domain logic**, pass **all eight hard-case automated tests**, and run unit/API/database integration tests; a failing required test causes the build to fail. | **Must** |

**Performance boundary:** ElectroCO's normal **under-1-second** and AquaCo's **2–4-second** responses are **external mock-biller behaviors**, not promises of 800 ms end-to-end response. PayLink's 800 ms proposed latency applies only to non-provider-dependent internal APIs. The brief's separate throughput requirement is **100 payments/second for one minute against mocks**.

---

## 6. Biller-Specific Payment Workflows

### 6.1 ElectroCO - Electricity Payments

**Protocol:** REST / JSON  
**Inquiry:** Meter number  
**Provider Behavior:** Fast response, under 1 second; immediate payment confirmation.

1. Customer selects ElectroCO.
2. Customer enters the electricity meter number.
3. PayLink sends a REST/JSON inquiry request.
4. ElectroCO returns the electricity bill information.
5. Customer reviews the bill and authorizes payment.
6. PayLink reserves wallet funds and sends the payment request.
7. ElectroCO immediately returns the payment result.
8. After `FundsHeld -> SentToBiller`, confirmed success leads to `Completed` with a finalized debit; a definitive refusal leads to `Failed -> Refunded` after the hold is released. A missing answer at **10 seconds** leads to `Unknown` with funds still held.

### 6.2 AquaCo - Water Payments

**Protocol:** SOAP-style XML over HTTP  
**Provider Behavior:** 2-4 second response, provider-specific fields and errors, no payments above the bill amount.

1. Customer selects AquaCo.
2. Customer enters the water billing account information.
3. PayLink converts the inquiry into the AquaCo XML format.
4. AquaCo responds using its field names and error codes.
5. PayLink maps the response into its standard bill format.
6. The system checks the outstanding bill amount and rejects any attempted overpayment.
7. Customer authorizes the payment.
8. PayLink sends a SOAP-style XML payment request.
9. PayLink processes the response and maps any provider-specific error.
10. `SentToBiller -> Completed` finalizes the debit only after confirmed success; a definitive refusal transitions `SentToBiller -> Failed -> Refunded` and releases the hold.

### 6.3 TopUpNet - Mobile Top-Up

**Protocol:** REST / JSON, asynchronous  
**Provider Behavior:** Returns `accepted`, then calls PayLink's callback URL minutes later with `success` or `failed`.

1. Customer selects TopUpNet.
2. Customer enters a mobile number and recharge amount.
3. Customer authorizes the payment.
4. PayLink reserves wallet funds and sends a REST/JSON top-up request.
5. TopUpNet returns `accepted`.
6. PayLink retains the canonical `SentToBiller` state and full hold; `accepted` is not `Completed`.
7. If there is no definitive response at **10 seconds**, PayLink transitions `SentToBiller -> Unknown` and re-queries the original provider reference at **1, 5, and 15 minutes**; TopUpNet sends a later callback to PayLink.
8. PayLink authenticates the callback and matches the transaction.
9. If the authenticated callback reports `success`, PayLink transitions `SentToBiller/Unknown -> Completed` and finalizes the debit once.
10. If the authenticated callback reports `failed`, PayLink transitions to `Failed`, releases the hold, then transitions `Failed -> Refunded`.
11. Duplicate callbacks must be **ignored for financial/state effects and logged**.

### 6.4 NetCo - Internet Bills

**Protocol:** Daily batch CSV file  
**Provider Behavior:** No live inquiry; PayLink uploads payment records once daily and receives results the following morning.

1. Customer selects NetCo.
2. Customer enters the required internet account reference and payment amount.
3. PayLink validates the input locally without making a live NetCo inquiry.
4. Customer authorizes the payment.
5. PayLink holds wallet funds (`Initiated -> FundsHeld`) and records **queued** as internal batch metadata, not a canonical payment state.
6. At the daily cutoff, PayLink generates the NetCo payment CSV file.
7. PayLink uploads the daily CSV through the configured exchange mechanism; once submission is confirmed, it transitions `FundsHeld -> SentToBiller`.
8. The canonical payment state remains `SentToBiller` while results are awaited, with a customer-facing "processing" message. The next-morning result deadline is configured separately from the **10-second live-biller timeout**.
9. The next morning, PayLink receives and imports the NetCo result CSV.
10. Each result is matched to its original transaction.
11. Verified successful results transition `SentToBiller/Unknown -> Completed` and finalize the wallet debits once.
12. Verified refusal results transition to `Failed`, then to `Refunded` after holds are released.
13. Missing or unmatched results at the result deadline cause `SentToBiller -> Unknown` (or remain `Unknown`), keep holds, and are flagged for reconciliation. Ambiguous file uploads must not be blindly repeated.

---

## 7. Payment Status Model

### 7.1 Mandatory Canonical Payment States (Assignment Section 2.5)

**The canonical payment state must be exactly one of the eight brief-defined values:** `Initiated`, `FundsHeld`, `Rejected`, `SentToBiller`, `Completed`, `Failed`, `Unknown`, `Refunded` (**eight** canonical states). There are **no other valid canonical states or transitions**. `Completed`, `Rejected` and `Refunded` are terminal.

| From | Trigger (exact brief semantics) | To | Wallet/ledger consequence |
|---|---|---|---|
| `Initiated` | Fraud checks pass **and** wallet has enough funds | `FundsHeld` | Atomically create the hold: transfer full bill+fee from available to held. |
| `Initiated` | Fraud check fails **or** funds are insufficient | `Rejected` | No hold, no final debit; reason recorded. |
| `FundsHeld` | Request is sent to biller | `SentToBiller` | Keep hold; sending is **not** success. |
| `SentToBiller` | Biller confirms payment | `Completed` | Convert hold to a final debit; post fee income and one agent commission when applicable. |
| `SentToBiller` | Biller definitively refuses | `Failed` | Failure is recorded; hold is **not yet released** until the next transition. |
| `SentToBiller` | No definitive answer within applicable timeout | `Unknown` | Retain hold; do **not** refund. |
| `Unknown` | Biller later confirms by callback, status re-query or settlement file | `Completed` | Finalize held money exactly once. |
| `Unknown` | Biller later refuses, **or** reconciliation confirms no payment | `Failed` | Record definitive failure; hold still present until release. |
| `Failed` | Hold is released to wallet | `Refunded` | Transfer held money back to available via ledger entries exactly once. |

**Invariants:** Every payment is in exactly one canonical state. The transition graph is closed (no implicit `Completed -> Refunded`, `Rejected -> Initiated`, `Failed -> Completed`, or direct `Unknown -> Refunded`). Every money movement creates exact-decimal ledger entries. An `Unknown` payment **never triggers an automatic refund** merely because of a timeout. If a late settlement contradicts a terminal failure/refund, flag a reconciliation mismatch for support instead of silently reopening the state.

### 7.2 Internal and Customer-Facing Statuses (not canonical states)

| Previous Version 1.6 status | Treatment | Mapping / rule |
|---|---|---|
| `Processing` | **UI display label only** | Use while canonical `Initiated`, `FundsHeld`, or `SentToBiller`; never persist as canonical payment state. |
| `Queued` | **NetCo internal batch flag** | A queued payment stays canonical `FundsHeld` until file submission; batch metadata `queued/uploading/uploaded` is tracked separately. |
| `Pending` | **UI display label only** | Display for `SentToBiller` awaiting confirmation or `Unknown`; do not replace the canonical state. |
| `Successful` | **UI synonym only** | Canonical `Completed`. |
| `Failed` | **Canonical** | Only a transitional state followed by `Failed -> Refunded` once hold is released. |
| `Refunded` | **Canonical** | Final state for the release of a **failed payment's held funds**. |

**TopUpNet:** `accepted` is a non-final acknowledgment. After request dispatch, stay `SentToBiller`; at the **10-second live-biller timeout** with no final answer, move to `Unknown`, keep funds held, and accept authenticated/deduplicated later callbacks. **NetCo:** queued batch rows stay `FundsHeld`; upload confirmation moves them to `SentToBiller`. No result by the configured **next-morning batch deadline** moves an unresolved sent row to `Unknown`; no automatic hold release. NetCo is **not** a 10-second live biller.

**Completed-payment refund request (brief FR-27, Could):** because `Completed` is terminal in the mandatory payment state machine, handle a later approved refund using a **separate, linked compensating financial/refund record**; do not transition the original `Completed` payment to `Refunded`. The refund request's own approval/settlement status is not a canonical payment state.

---

## 8. Assumptions and Outstanding Clarifications

**Decision governance:** "Confirmed" means explicitly specified in the university brief or selected by the user in this project; it does **not** mean the implementation team independently approved the policy. "Proposed" identifies a reasonable engineering assumption that still needs team review. "Pending Clarification" identifies an unresolved decision. No assistant-generated proposal is recorded as a confirmed team decision.

| Ambiguity ID | Question / unclear requirement | Proposed decision | Reason | Decision status |
|---|---|---|---|---|
| AMB-01 | Who owns and maintains wallet funds and the ledger? | PayLink owns its internal wallet and immutable exact-decimal ledger. | The user explicitly chose an in-house wallet; the brief mandates held/available balances and ledger movements. | **Confirmed** (user and brief) |
| AMB-02 | Which funding channels are included? | Bank cards, bank transfers and authorized cash agents; external processors only provide funding confirmation. | User explicitly selected these three channels. | **Confirmed** (user choice) |
| AMB-03 | Are all four billers required in the team's implementation? | Specify ElectroCO, AquaCo, TopUpNet and NetCo; prioritize NetCo `Should` because the brief explicitly makes the batch biller optional. | User requested four; the brief mandates the first three and labels NetCo optional. | **Confirmed scope / Proposed priority** |
| AMB-04 | Should duplicate bill references be blocked even with a new idempotency key? | Deduplicate by the same key for all payment types; also prevent repeated payment for a **unique, biller-verified bill reference** while an active/completed payment exists. Do not block legitimate repeated mobile top-ups merely because number/amount match. | Prevent accidental double-charge without forbidding legitimate top-up repeats. | **Proposed** |
| AMB-05 | Which transaction limit applies to principal versus total including fees? | Enforce brief's **5–10,000 EGP** single-payment range on principal, **5,000/30,000 EGP** daily customer spend using total wallet deduction (including fees), and full available balance against principal plus fee. | Exact counting convention is not stated; conservatively protects money while retaining fixed values. | **Pending Clarification** |
| AMB-06 | What does agent cash confirmation require? | Authenticate active agent, confirm cash was received, check funded/settlement-backed allowance, atomically consume allowance, log actor and credit once. | Prevents agents from minting unbacked electronic funds. | **Proposed** |
| AMB-07 | What happens when TopUpNet sends the same or contradictory callback twice? | Authenticate callback, check provider reference, apply first valid final result once; ignore/log duplicate and quarantine contradictory/invalid notifications. | Brief HC-06 explicitly requires ignored, logged duplicate `success` callback. | **Confirmed outcome / Proposed authentication method** |
| AMB-08 | How should missing NetCo upload acknowledgments and incomplete next-day files be handled? | Stable batch ID, reconcile ambiguous upload before resend; rows stay held, and missing results at a configured next-morning deadline become `Unknown` with reconciliation alert. | Avoids double payment or unsafe release when the provider result is unknown. | **Proposed** |
| AMB-09 | Does a refund of a completed payment change its mandatory terminal state? | Keep original `Completed` immutable and create a linked compensating refund record after support approval; `Failed -> Refunded` releases held funds. | The brief defines `Completed` as terminal and `Failed -> Refunded` as the only canonical refund transition. | **Confirmed state constraints / Proposed compensation design** |
| AMB-10 | What currency and rounding must be used? | **EGP**, exact decimal monetary arithmetic, consistently configured smallest currency unit and rounding policy. | EGP and exact-decimal arithmetic are specified in brief Sections 2.3 and 2.6. | **Confirmed currency/type; Pending Clarification rounding mode** |
| AMB-11 | What must happen after a 10-second live timeout? | `SentToBiller -> Unknown` with hold retained, query same reference at **1, 5, 15 minutes** only; then wait for reconciliation. | Mandatory brief transition and fixed retry schedule. | **Confirmed** (brief) |
| AMB-12 | How do fee promotions and commission apply? | Apply exactly the brief fee formula; promotion zeroes fee for chosen biller/dates; agent gets 40% of **earned** fee on completed agent-assisted payment (zero if promotion fee zero). | Brief fixes fees, promotion and agent commission. | **Confirmed formulas; Proposed commission interpretation** |
| AMB-13 | How will a non-query NetCo amount be determined? | Allow a customer-entered amount explicitly labeled **unverified by NetCo**, subject to limits; no statement that PayLink fetched a bill amount. | NetCo provides no live inquiry and the brief does not provide an alternative bill source. | **Pending Clarification** |
| AMB-14 | Are exact callback signatures, AquaCo error mappings, NetCo CSV columns and cutoff known? | Provide per-biller adapter configuration with test contracts and mock fixtures; ask team/instructor to specify exact fields and deadlines. | Brief describes behavior/protocol but not full schemas, signatures or timing for batch cutoff. | **Pending Clarification** |
| AMB-15 | How should a settlement-file record contradict a final PayLink state? | Flag a reconciliation discrepancy for support; do not reverse final state or charge again automatically. | Directly required by mandatory HC-07. | **Confirmed** (brief) |
| AMB-16 | How should SMS outage affect payment success? | Notification delivery is independent; retry SMS **three times**, then mark undelivered. | Brief HC-05 and fixed notification retry rule. | **Confirmed** (brief) |

### Questions for team / instructor review

1. Does the **daily limit** include PayLink's service fee, or only the bill/top-up principal (AMB-05)?
2. What exact **agent settlement/allowance verification** authorizes an immediate cash-to-wallet credit (AMB-06)?
3. What is the **NetCo result-file deadline**, upload acknowledgment mechanism, file schema and handling of partial rejections (AMB-08/AMB-14)?
4. What data source determines the **NetCo bill amount**, if customer-entered amounts are not allowed (AMB-13)?
5. Which rounding mode applies to percentage fees/commissions at fractional-piaster values (AMB-10)?
6. Which callback authentication mechanism and message IDs does the TopUpNet mock support (AMB-07/AMB-14)?

---

## 9. Requirements Summary

| Component | Count / status |
|---|---:|
| Stakeholders (unchanged) | **9** |
| Supported billers | **4** (NetCo optional in brief but included in project scope) |
| Wallet funding methods | **3** (bank cards, bank transfers, cash agents) |
| Functional requirements (local IDs) | **30** (FR-01 through FR-30) |
| Explicitly ID'd user stories | **30** (US-01 through US-30) |
| Mandatory hard cases with matching acceptance scenarios | **8 / 8** |
| Measurable non-functional requirements | **12** (NFR-01 through NFR-12) |
| Functional Must priorities | **26** |
| Functional Should priorities | **3** |
| Functional Could priorities | **1** |
| Canonical payment states and allowed transitions | **8 states; 9 transitions**, exactly as brief Section 2.5 |

**Document status:** Reviewed and updated requirements draft, Version 1.7. Priorities, fixed business rules, traceability, and hard-case scenarios are aligned to *PayLink Year Work Project - Student Brief*, Sections **2.4–2.7**, **3.1–3.3**, and **4 (Phase 1 requirements pack)**. Pending policy choices are marked as such, not represented as team approvals.

---

## 10. Requirements Traceability Matrix

**Interpretation:** Local `FR-XX` and `US-XX` are from this Version 1.7 document. **Brief FR-N** refers to the independent numbering in assignment Section 3.2. `HC-01`–`HC-08` are exactly the **eight** original situations in Section 2.7. "—" means no direct hard-case association, not a missing user story. A hard case is counted only if its matching Given/When/Then scenario is present in the local FR.

| Local FR | User Story ID | Relevant original assignment requirement | Hard Case | Priority |
|---|---|---|---|---|
| FR-01 | US-01 | Brief FR-1 (Must); §2.3 | — | **Must** |
| FR-02 | US-02 | Brief FR-2 (Should); FR-1 (Must), §2.2 | — | **Must** |
| FR-03 | US-03 | Brief FR-3 (Must); §2.3 | — | **Must** |
| FR-04 | US-04 | Brief FR-4 (Must) | — | **Must** |
| FR-05 | US-05 | Brief FR-4 (Must); additional bank-transfer channel (user-selected) | — | **Must** |
| FR-06 | US-06 | Brief FR-4 (Must); FR-17 (Should); §2.6 commission | — | **Must** |
| FR-07 | US-07 | Brief FR-4, FR-5 (Must) | — | **Must** |
| FR-08 | US-08 | Brief FR-3, FR-5, FR-25 (Must) | — | **Must** |
| FR-09 | US-09 | Brief FR-12, FR-18 (Must); FR-19, FR-21 (Should) | HC-04 | **Must** |
| FR-10 | US-10 | Brief FR-6, FR-7 (Must) | — | **Must** |
| FR-11 | US-11 | Brief FR-7 (Must); §3.1 Biller Catalog | — | **Must** |
| FR-12 | US-12 | Brief FR-8, FR-15 (Must); §2.4 ElectroCo | — | **Must** |
| FR-13 | US-13 | Brief FR-8, FR-15 (Must); §2.4 AquaCo | — | **Must** |
| FR-14 | US-14 | Brief FR-7, FR-15 (Must); §2.4 TopUpNet | — | **Must** |
| FR-15 | US-15 | Brief FR-15 (NetCo optional); §2.4 NetCo | — | **Should** |
| FR-16 | US-16 | Brief FR-9, FR-18 (Must); FR-19 (Should); §2.6 | — | **Must** |
| FR-17 | US-17 | Brief FR-5, FR-9, FR-11, FR-12 (Must); §2.5 | — | **Must** |
| FR-18 | US-18 | Brief FR-10 (Must); §2.7 | HC-01 | **Must** |
| FR-19 | US-19 | Brief FR-11, FR-13, FR-15 (Must); §2.5, §2.7 | HC-02 | **Must** |
| FR-20 | US-20 | Brief FR-11, FR-12, FR-15 (Must); §2.4, §2.7 | HC-03 | **Must** |
| FR-21 | US-21 | Brief FR-11, FR-13, FR-14, FR-15 (Must); §2.4 | — | **Must** |
| FR-22 | US-22 | Brief FR-11, FR-14 (Must); §2.7 | HC-06 | **Must** |
| FR-23 | US-23 | Brief FR-15 (NetCo optional); §2.4 NetCo | — | **Should** |
| FR-24 | US-24 | Brief FR-15 (NetCo optional); §2.4 NetCo | — | **Should** |
| FR-25 | US-25 | Brief FR-5, FR-9, FR-11, FR-12, FR-16 (Must); FR-17 (Should) | — | **Must** |
| FR-26 | US-26 | Brief FR-11, FR-13 (Must); FR-26 (Should); §2.7 | HC-07 | **Must** |
| FR-27 | US-27 | Brief FR-22 (Must); FR-23 (Should); §2.6 notifications | HC-03, HC-05 | **Must** |
| FR-28 | US-28 | Brief FR-20 (Must); FR-21 (Should); FR-27 (Could) | — | **Must** |
| FR-29 | US-29 | Brief FR-6, FR-16 (Must); FR-24 (Should); §2.4 v2 change | HC-08 | **Must** |
| FR-30 | US-30 | Brief FR-28 (Could); FR-26 (Should); §2.6 settlement | HC-07 | **Could** |

---

## 11. Mandatory Hard Cases - Acceptance and Automated Test Crosswalk

**All eight cases are the original numbered situations in assignment Section 2.7; no new mandatory hard cases have been introduced.** Each row identifies a **specific Given/When/Then acceptance scenario** above and an executable-test expectation for implementation. Automated tests are requirements for the future codebase; this document does not claim tests were executed.

| Original HC | Brief situation | Explicit acceptance scenario | Expected automated test assertion |
|---|---|---|---|
| **HC-01** | Duplicate mobile app request due to slow network | **FR-18 / AC-18-B** | Same key/payload returns original transaction and result; exactly one hold/debit. |
| **HC-02** | ElectroCO responds only after 12 seconds | **FR-19 / AC-19-B** | At 10 seconds: `SentToBiller -> Unknown`, hold stays, customer sees processing. |
| **HC-03** | AquaCo refuses with error code | **FR-20 / AC-20-C** + **FR-27 / AC-27-E** | Proper error reason, `Failed -> Refunded` hold release and SMS notification. |
| **HC-04** | Two concurrent requests exceed one-wallet funding | **FR-09 / AC-09-B** | Exactly one goes `FundsHeld`; other `Rejected`; no negative available balance. |
| **HC-05** | SMS service down during completion | **FR-27 / AC-27-D** | Payment stays `Completed`; SMS retried 3 times then undelivered. |
| **HC-06** | Duplicate TopUpNet success callback | **FR-22 / AC-22-C** | First completes once; duplicate ignored for money/state and logged. |
| **HC-07** | Night settlement lists payment PayLink marked Failed | **FR-26 / AC-26-C** | Discrepancy appears in reconciliation report for support; no auto re-charge. |
| **HC-08** | A new biller must be added | **FR-29 / AC-29-D** | Only new Biller Integration adapter/config changes; existing modules untouched. |

**Coverage verification:** **8/8** mandatory hard cases have a labeled acceptance scenario. The relevant Given/When/Then descriptions are in Section 4; runtime pass/fail evidence belongs in the later automated test suite.

---

## 12. Assignment Technical and Delivery Trace (Non-FR Constraints)

This is a brief-facing checklist, **not** an assertion that software has been implemented or submitted. It records assignment constraints that cannot be satisfied by a requirements document alone:

| Brief reference | Required evidence / design constraint | Requirements connection |
|---|---|---|
| §3.1 | At least **6** modules, each with public interface and tests: Accounts & Wallet; Biller Catalog; Payment Processing; Biller Integration; Fees & Commissions; Notifications. Fraud Checks and Reconciliation are recommended extras. | FR-01–FR-30, especially FR-16, FR-26–FR-29. |
| §3.4 | Relational database with **at least 10 tables**, migrations in repository. | Wallet ledger, payment state, biller configuration and audits. |
| §3.4 | **At least 20 REST endpoints** documented using OpenAPI and **one asynchronous flow**. | TopUpNet callback (FR-22); NetCo batch (FR-23/24). |
| §3.4 | **5 distinct design patterns** used for genuine problems (e.g. Adapter, Factory, Strategy, State, Chain of Responsibility); architecture: layered or hexagonal modular monolith for Phase 1. | Integration adapters, fee engine, canonical state machine, fraud rules. |
| §3.4 / Phase 2 | ElectroCO **v2** field-name changes, EGP amounts changed to piasters, request signing, isolated to adapter/config only. | NFR-11; FR-29 / HC-08. |
| §3.4 / Phase 2 | Dockerfiles and docker compose for app, DB and mock billers; GitHub Actions builds, lints, tests, reports coverage and supports local deployment. | NFR-12 and implementation deliverables. |
| Phase 1 §4 | Requirements pack as **PDF plus `/docs/requirements.md`**, 25+ user stories, 6+ measurable NFRs, priorities and ambiguity decisions. | This Version 1.7 requirements specification. |
| Phase 1 §4 | Design pack: use case, domain class, pay/refund sequence, exact payment state and ER diagrams as **PlantUML/Mermaid source files**. | Section 7 is the diagram's required state source; separate diagram files still needed. |
| Phase 1 §4 | Prompt log **5+ entries**, including **2 corrected initial AI answers**; repository/CI skeleton. | Separate deliverables; not implied to be included here. |
| Phase 2 §4 | Unit/integration tests, automated test for all 8 hard cases, **70%+** domain coverage, 5 patterns and CI/CD evidence. | Section 11, NFR-12; actual evidence is not in this document. |

