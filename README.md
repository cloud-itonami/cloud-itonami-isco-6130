# cloud-itonami-isco-6130

Open Occupation Blueprint for **ISCO-08 6130**: Mixed Crop and Animal Producers.

This repository designs a forkable OSS business for an independent smallholder mixed farmer: a field-and-barn robot performs crop tending and livestock-health monitoring under a governor-gated actor, so the farmer keeps their own operating records instead of renting a closed farm-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a field-and-barn robot performs crop tending, feed distribution and livestock-health monitoring under an actor that proposes
actions and an independent **Mixed Farm Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near livestock, or applying treatments near water sources) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
farm plan + crop plan + livestock health protocol
        |
        v
Farm Advisor -> Mixed Farm Governor -> tend/harvest, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6130`). Required capabilities:

- :robotics
- :telemetry
- :optimization
- :dmn
- :bpmn
- :audit-ledger
- :forms

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

Unlike earlier `cloud-itonami-isco-*` reference actors (a standalone
`Store` + pure `assess` governor function), this repository implements
the **full itonami Actor pattern** from CLAUDE.md's Actors section: a
real [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/mixed_farming/store.cljc` — `Store` protocol + `MemStore`:
  registered plots (`has-livestock?`, `near-water?`), committed
  records, an append-only audit ledger.
- `src/mixed_farming/advisor.cljc` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes a farm operation from a request;
  `llm-advisor` wraps a `langchain.model/ChatModel` — either way the
  advisor only ever produces a `:propose`-effect proposal, never a
  committed record, and LLM parse failures always yield `confidence 0.0`
  (forces escalation, never fabricated confidence).
- `src/mixed_farming/governor.cljc` — `FarmOpsGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants (unregistered
  plot, a proposal whose `:effect` isn't `:propose`) always route to
  `:hold`. Escalation invariants (chemical treatment on a
  `near-water?` plot, any non-feed/harvest op on a `has-livestock?`
  plot, or low advisor confidence) always route to `:request-approval`
  — an `interrupt-before` node that the graph checkpoints and only
  resumes on explicit human approval (`actor/approve!`).
- `src/mixed_farming/actor.cljc` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test   # 10 tests, 27 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the first `cloud-itonami-isco-*` occupation actor built on the full
`langgraph.graph`/`langchain` Actor pattern (prior 23 implemented
occupations use the lighter Store+governor-only pattern).

## License

AGPL-3.0-or-later.
