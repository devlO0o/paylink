# Payment State Diagram

Source: brief section 2.5 (Payment life cycle). Only the transitions below are allowed (FR-11).

```mermaid
stateDiagram-v2
    direction LR

    [*] --> Initiated : customer submits payment (idempotency key)

    Initiated --> FundsHeld : fraud checks pass and funds sufficient
    Initiated --> Rejected : fraud check fails or funds insufficient

    FundsHeld --> SentToBiller : request sent to biller

    SentToBiller --> Completed : biller confirms
    SentToBiller --> Failed : biller refuses
    SentToBiller --> Unknown : no answer within timeout (10 s)

    Unknown --> Completed : biller later confirms (callback, retry or settlement file)
    Unknown --> Failed : biller later refuses, or reconciliation finds no payment

    Failed --> Refunded : hold released to wallet

    Completed --> [*]
    Rejected --> [*]
    Refunded --> [*]

    note right of FundsHeld
        Amount + fee moved from
        available to held balance
    end note

    note right of Unknown
        Money stays held. Re-queried after
        1, 5 and 15 min, then waits for
        reconciliation. Never auto-refunded.
    end note

    note right of Completed
        Hold becomes final charge, fee income
        credited, agent commission (40% of fee)
        recorded, SMS + push sent
    end note
```

## Transition table

| From | Event | To |
|---|---|---|
| Initiated | Fraud checks pass and wallet has enough funds | FundsHeld |
| Initiated | Fraud check fails or funds are insufficient | Rejected |
| FundsHeld | Request sent to biller | SentToBiller |
| SentToBiller | Biller confirms | Completed |
| SentToBiller | Biller refuses | Failed |
| SentToBiller | No answer within the timeout | Unknown |
| Unknown | Biller later confirms (callback, retry or settlement file) | Completed |
| Unknown | Biller later refuses, or reconciliation finds no payment | Failed |
| Failed | Hold released to the wallet | Refunded |

Final states: **Completed**, **Rejected**, **Refunded**. Any event not in this table is rejected by the State pattern implementation (e.g. a duplicate TopUpNet "success" callback on a Completed payment is ignored and logged — hard case 6).
