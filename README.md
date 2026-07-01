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

## License

AGPL-3.0-or-later.
