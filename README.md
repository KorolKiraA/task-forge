# TaskForge

![Scala](https://img.shields.io/badge/Scala-3-red) ![License](https://img.shields.io/badge/license-MIT-blue) ![Dependencies](https://img.shields.io/badge/dependencies-none-brightgreen)

A priority-queue ticket engine — the core of a helpdesk/ITSM system.
Tickets carry SLA deadlines per priority, critical incidents are always
served first, and overdue items escalate automatically. SLA compliance is
checked on every resolution.

## Features

- 4 SLA priority levels: Critical (4 h) · High (24 h) · Medium (72 h) · Low (168 h)
- Priority queue with FIFO tie-breaking — critical tickets surface first
- Automatic escalation warnings for SLA breaches on every action
- Engineer assignment and resolution with SLA pass/fail verdict
- Backlog metrics: open tickets, critical share, overdue count

## Quick start

Requires **Scala 3** via [scala-cli](https://scala-cli.virtuslab.org/):

```bash
scala-cli run TaskForge.scala
```

## Sample output

```
⚠ ESCALATION: 2 tickets breached SLA! Notify the shift lead.

ID     Priority   SLA due            Engineer     Ticket
1001   Critical   07.11.2026 21:00   —            Payment gateway is down
1002   High       08.11.2026 17:00   kirill       Clients not receiving emails
```

## Tech stack

Scala 3 · `PriorityQueue` · parameterized enums · immutable case classes.

## License

MIT — see [LICENSE](LICENSE).
