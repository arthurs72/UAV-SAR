# UAV-SAR: Multi-Agent Search and Rescue System

## Overview

This project implements an agent-based UAV search and rescue (SAR) model.

Target architecture direction:
- BDI-style high-level state behavior
- ACO-inspired local coordination and movement selection
- TAPB-style belief freshness checks

Current objective is a stable, reproducible baseline that can be extended into full BDI+ACO+TAPB behavior and sensitivity experiments.

---

## Core Idea

Agents interact through a **shared environment** using:

* attraction to potential targets
* repulsion to avoid overlapping search

This enables emergent coordination and distributed exploration.

---

## Agent Behaviour

Each agent follows a general lifecycle:

* **Explore**: search the environment
* **Exploit**: investigate potential targets
* **Return**: manage resources (e.g. battery)
* **Adapt**: adjust behaviour under changing conditions

This reflects an exploration–exploitation trade-off in a dynamic environment.

---

## Current Implementation Status

Implemented and validated:
- AnyLogic model opens and runs from `src/UAV-SAR/UAV-SAR.alpx`.
- UAVs move continuously across the map area (bounded random waypoint baseline within the active search region).
- UAV statechart has guarded Explore/Exploit transitions based on:
	- `varTargetConfidence` vs `varDetectionThreshold`
	- `varBatteryLevel` vs `varReturnBatteryThreshold`
- Victim spawning uses robust GMM placement with in-bounds rejection/fallback.
- Simulation stop behavior is stable (no dynamic-event destruction errors in the current baseline flow).

Not fully implemented yet:
- TAPB stale-belief cycle (`SenseIfOutdated` as a full reasoning step).
- ACO local move scoring as the primary Explore policy (current movement baseline is bounded random waypoint chaining).
- Full experiment logging/export pipeline execution and result analysis automation.

---

## How To Run (AnyLogic)

### Prerequisites

- AnyLogic 8.9.x installed.
- Repository cloned locally.

### Run Steps

1. Open AnyLogic.
2. Open project file: `src/UAV-SAR/UAV-SAR.alpx`.
3. Open experiment: `Simulation`.
4. Click **Run**.
5. Observe UAV movement and victim interactions in the animation window.

Notes:
- Simulation experiment stop option is `Never`; stop manually when needed.
- Experiment uses fixed seed (`500000`) for baseline reproducibility.

Tuning (quick experiment knobs):
- In the Main agent Variables, adjust: 
  `varUseAcoMoveDefault`, `varAcoCandidateCountDefault`,
  `varAcoStepLengthDefault`, `varAcoUavAvoidRadiusDefault`, `varUavPushRateDefault`,
  `varRandomWeightDefault`, `varSensorRangeDefault`, `varBeliefStaleSecondsDefault`.

---

## Key Config And Data Paths

- Example experiment: `config/example_experiment.json`
- Baseline sensitivity sweep: `config/sweeps/baseline_sensitivity.json`
- Sweep conventions: `config/sweeps/README.md`
- Per-run/aggregate schema docs: `data/schema/`

---

## Project Structure

- `src/UAV-SAR/UAV-SAR.alpx`: AnyLogic workspace file
- `src/UAV-SAR/_alp`: generated AnyLogic source
- `src/UAV-SAR/_alp/Agents/Main/Code/Functions.java`: victim GMM placement
- `src/UAV-SAR/_alp/Agents/UAV/AOC.UAV.xml`: UAV startup + statechart guards
- `src/UAV-SAR/_alp/Agents/UAV/Code/Functions.java`: UAV waypoint movement callbacks
- `docs/`: planning and method notes
- `config/`: experiment/sweep definitions

---

## Next Technical Step

Replace random waypoint Explore movement with explicit TAPB + ACO decision flow:
1. Sense/refresh stale beliefs.
2. Score local movement options using ACO-style heuristic + pheromone terms.
3. Select and execute move, then deposit/update local trace.

This will align the running model with the intended research narrative for report and presentation.
