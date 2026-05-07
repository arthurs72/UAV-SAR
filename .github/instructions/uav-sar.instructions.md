---
name: "UAV-SAR AnyLogic Implementation"
description: "Use when implementing UAV-SAR model logic, AnyLogic state behaviour, experiment configs, sensitivity sweeps, and reproducible CSV outputs."
applyTo: "src/UAV-SAR/**, config/**/*.json, data/**/*.csv, analysis/**/*.ipynb, tests/**"
---

# UAV-SAR Implementation Instructions

## Objective
Implement the smallest reproducible UAV-SAR prototype that reveals emergent behaviour from local rules and supports behavioural sensitivity experiments.

## Platform and Scope
- Primary platform is AnyLogic.
- Keep model logic compatible with the existing `UAV-SAR.alpx` structure.
- Prefer concise, explicit state/rule logic over large abstractions.

## Mandatory Behaviour Building Blocks
1. Victim spawning
- Support GMM cluster-based placement with configurable:
  - number of clusters
  - cluster weights
  - spread per cluster
  - margin from boundaries
- Keep in-bounds guarantees via rejection or clipping checks.

2. UAV initialization
- Random in-bounds spawn, controlled by experiment seed.

3. Baseline policies
- Implement `greedy_nearest`, `lawnmower`, and `partitioning`.
- Keep each policy selectable by config.

4. BDI-oriented intentions
- Maintain explicit `Explore`, `Exploit`, and `Return` logic.
- Ensure transitions are interpretable in logs and presentation visuals.
- Implement this minimum internal cycle for `Explore`:
  - `SenseIfOutdated` (TAPB check and active perception if beliefs are stale)
  - `SelectMoveACO` (choose next move from local pheromone beliefs)
  - `MoveAndDeposit` (execute move and deposit/update traces)

State transition guards:
- `Target detected` -> `Exploit`
- `Battery low` -> `Return`
- `False alarm` -> `Explore`
- `Mission complete` -> `Return`

Optional v2 extensions (only after baseline is stable):
- `Communication` sub-state in `Explore` (opportunistic map merge)
- `Degraded` state for partial failures

5. Sensor models
- `probabilistic`: distance-decay detection probability.
- `deterministic`: fixed radius threshold.
- Expose toggle in config.

## Reproducibility Contract
- Read one top-level `seed` per run from config.
- Derive all randomness from that seed only.
- Same config + same seed must reproduce same results.

## Data and Logging Contract
Create two levels of CSV output:
1. Per-run step metrics in `data/runs/`.
2. Aggregate run metrics in `data/sweeps/`.

Minimum aggregate metrics:
- `time_to_all_found`
- `final_coverage_ratio`
- `final_redundancy_ratio`
- `total_energy_used`
- `success_flag`

## Experiment Design Rules
- Default to 30 seeds per configuration.
- Vary one intervention parameter at a time in early sweeps.
- Include victim-distribution sensitivity (uniform vs clustered/GMM).
- Report variability, not just single-run outcomes.

Prioritization note:
- Prefer a complete, reproducible baseline over a complex architecture.
- Treat communication/degraded behavior as optional extensions for additional insight.

## Testing Rules
- Include at least one automated test for victim spawner correctness.
- Validate bounds safety and seeded reproducibility.

## Presentation Alignment
When producing figures/tables/scripts, prioritize:
- traceable micro-rules,
- visible macro-patterns,
- clear sensitivity comparisons,
- explicit limitations.
