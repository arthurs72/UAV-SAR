# LLM Instructions for This Repository

This is an AnyLogic simulation project using the **split `.alpx` format**.
Before touching any project files, read this entire document.

---

## Do NOT modify project files directly if you can avoid it

The safest workflow is:
1. Make the user apply changes through the AnyLogic UI instead.
2. If you must edit files, **AnyLogic must be completely closed first**. If AnyLogic is open while you write files, it will detect the external modification and regenerate the files from its in-memory state, silently wiping your changes.

---

## How the split `.alpx` format works

The project lives in two places that must stay in sync:

| Path | Purpose |
|------|---------|
| `src/UAV-SAR/UAV-SAR.alpx` | Top-level workspace descriptor (do not edit) |
| `src/UAV-SAR/_alp/` | All actual model content (agents, variables, functions, etc.) |

Each agent has its own subfolder under `_alp/Agents/<AgentName>/`.

---

## How functions are stored

Functions are split across **two files** per agent:

### `_alp/Agents/<Agent>/Code/Functions.xml`
Stores function **metadata**: name, return type, parameters, ID, and canvas position.

Key rules:
- Functions that have a body must have `<Body xmlns:al="http://anylogic.com"/>` — this is a presence marker telling AnyLogic the body exists in the Java file. Functions with no body have no `<Body>` element.
- Function parameters are stored as `<Parameter>` elements inside the `<Function>` block.
- The return type is stored in `<ReturnType>`. For void functions, `<ReturnModificator>` is `VOID`. For value-returning functions it is also `VOID` (confusingly) — the actual return type is in `<ReturnType>` (e.g. `int`, `double`).

```xml
<Function AccessType="default" StaticFunction="false">
    <ReturnModificator>VOID</ReturnModificator>
    <ReturnType>int</ReturnType>
    <Id>1780000003010</Id>
    <Name><![CDATA[fnCellIndex]]></Name>
    ...
    <Parameters>
        <Parameter>
            <Name><![CDATA[x]]></Name>
            <Type><![CDATA[double]]></Type>
        </Parameter>
        <Parameter>
            <Name><![CDATA[y]]></Name>
            <Type><![CDATA[double]]></Type>
        </Parameter>
    </Parameters>
    <Body xmlns:al="http://anylogic.com"/>
</Function>
```

**Always add parameters through the AnyLogic GUI** (the "Arguments" section in the function properties panel). If you edit the XML directly, AnyLogic will overwrite it the next time the model is saved.

### `_alp/Agents/<Agent>/Code/Functions.java`
Stores the **actual function body code**, delimited by ID markers:

```java
int fnCellIndex(double x, double y)
{/*ALCODESTART::1780000003010*/
// actual code here
/*ALCODEEND*/}
```

The ID in `ALCODESTART::ID` must match the `<Id>` in `Functions.xml`.
The Java file is the source of truth for code. The XML is the source of truth for metadata.

---

## CRITICAL: Line endings must be CRLF (`\r\n`)

AnyLogic writes all project files with **Windows CRLF (`\r\n`) line endings**.
If `Functions.java` has LF-only (`\n`) endings, AnyLogic will detect the mismatch on next open and **regenerate the file from scratch**, producing empty function stubs and wiping all code.

### This repository enforces CRLF via `.gitattributes`

The `.gitattributes` file at the repo root sets `eol=crlf` for all text files:

```
* text=auto eol=crlf
```

This means git will always check out files with CRLF on disk. You should not need to do anything special. But if function bodies disappear after a pull or branch switch, check the line endings first.

### If you need to write a Java file manually (AnyLogic must be closed first)

Use PowerShell to guarantee CRLF:

```powershell
$content = git show HEAD:src/UAV-SAR/_alp/Agents/UAV/Code/Functions.java
$lines = $content -split "`n"
$stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create)
$writer = New-Object System.IO.StreamWriter($stream, [System.Text.Encoding]::UTF8)
$writer.NewLine = "`r`n"
foreach ($line in $lines) { $writer.WriteLine($line.TrimEnd("`r")) }
$writer.Close(); $stream.Close()
```

Or use Python if available:
```python
with open(r'path\to\Functions.java', 'wb') as f:
    f.write(b'\r\n'.join(line.encode('utf-8') for line in lines) + b'\r\n')
```

### Verify line endings
```powershell
$bytes = [System.IO.File]::ReadAllBytes($path)
$crlf = 0; $lf = 0
for ($i = 0; $i -lt $bytes.Length; $i++) {
    if ($bytes[$i] -eq 10) {
        if ($i -gt 0 -and $bytes[$i-1] -eq 13) { $crlf++ } else { $lf++ }
    }
}
Write-Host "CRLF=$crlf  LF-only=$lf"  # LF-only must be 0
```

### If the repository gets renormalized to LF (e.g. after changing `.gitattributes`)

Run this with AnyLogic closed to restore CRLF everywhere:

```powershell
cd <repo-root>
git add --renormalize .
git restore .
```

Then verify the Java files before opening AnyLogic.

---

## What goes wrong and why

| Mistake | What AnyLogic does |
|--------|-------------------|
| `Functions.java` has LF-only endings | Detects external modification, regenerates Java from XML (empty bodies) |
| Write `Functions.java` while AnyLogic is open | AnyLogic overwrites it from in-memory state on next save |
| Remove `<Body xmlns:al=.../>` from XML | AnyLogic shows empty body in UI, treats function as having no code |
| Add CDATA content to `<Body>` element | AnyLogic ignores it; code must be in the Java file, not the XML |
| Function has parameters in Java but not in XML | Compile error: "method not applicable for arguments" |
| Function returns a value but set to "Just an action" in GUI | Compile error: "Void methods cannot return a value" |

---

## Function configuration in the AnyLogic GUI

When creating or editing a function in AnyLogic:

- **"Just an action"** = `void` return type. Use for functions that do something but return nothing.
- **"Returns value"** = specify a return type (`int`, `double`, `boolean`, etc.). The function body must have a `return` statement.
- **Arguments section** = where you add parameters (name + type). These must match what the function body code expects. Missing parameters cause "cannot be resolved to a variable" compile errors.

---

## Other model elements (variables, events, etc.)

Variables are stored in `_alp/Agents/<Agent>/Variables.xml`. Initial values are stored inline as CDATA. Only `Functions.java` uses the separate-file pattern with ALCODESTART markers.

The statechart is stored inline in `AOC.<AgentName>.xml` under `<StatechartElements>`. Transition conditions and timeout values can reference variables directly (e.g. `varScanDuration`, `varDetectionThreshold`). Check this file before declaring a variable "unused" — it may be referenced in a transition guard or timeout that is not visible in the function code.

---

## Canvas layout conventions

- The **blue rectangle** in the agent editor is the presentation boundary — it defines what gets drawn in the parent agent's view during simulation. Only visual elements (agent shapes, animated text labels like `confidenceText`) need to be inside it.
- Variables, functions, events, and the statechart can all be placed **outside** the blue box. Position on the canvas is purely organizational.
- Use **Text elements** (Presentation palette, `A` icon) as section headers to group related variables/functions visually.
- Use **borderless Rectangle shapes** with a label to create named group boxes.
- Use **Note elements** (General/Miscellaneous palette) for longer explanations on the canvas.

---

## Current agent structure

### UAV agent — variable groups
- **Movement**: `varSpeed`, `varDesiredHeadingDeg`
- **ACO Navigation**: `varUseAcoMove`, `varAcoCandidateCount`, `varAcoStepLength`, `varAcoAlpha`, `varAcoBeta`, `varAcoUavAvoidRadius`, `varAcoMinUavSeparation`, `varHumanPullRate`, `varUavPushRate`, `varRandomWeight`, `varPheromoneDepositionRate`
- **Sensor / Detection**: `varSensorRange`, `varDetectionThreshold`, `varTargetX`, `varTargetY`, `varTargetConfidence`, `varScanDuration`
- **Belief Decay**: `varBeliefStaleSeconds`, `varBeliefDecayPerSecond`, `varLastSenseTime`, `varLastBeliefUpdateTime`, `varEnableSenseDebug`
- **Grid / Coverage Map**: `varGridCols`, `varGridRows`, `varGridCellSize`, `varGridOriginX`, `varGridOriginY`, `varCellLastSenseTime`, `varCellConfidence`
- **Battery / Energy**: `varBatteryLevel`, `varReturnBatteryThreshold`, `varScanEnergyCost`, `varEnergyUsed`
- **Statistics**: `varVictimsConfirmed`

### UAV agent — function groups
- **Movement**: `onArrival`, `fnMoveToRandomWaypoint`, `fnMoveToAcoWaypoint`, `fnMoveAco`
- **Sensing & Grid**: `fnSenseIfOutdated`, `fnSenseNow`, `fnSenseNowGrid`, `fnCellIndex(double x, double y)`, `fnCellFreshness(int idx)`

### Main agent — variable groups
- **Victim Placement (GMM)**: `varNClusters`, `varGmmWeights`, `varGmmSd`, `varGmmMargin`
- **Pheromone Grid**: `varPheromoneGrid`, `varPheromoneTime`, `varPheromoneEvapRate`
- **Experiment Defaults**: `varUseAcoMoveDefault`, `varAcoCandidateCountDefault`, `varAcoStepLengthDefault`, `varAcoUavAvoidRadiusDefault`, `varUavPushRateDefault`, `varRandomWeightDefault`, `varSensorRangeDefault`, `varBeliefStaleSecondsDefault`, `varAcoMinUavSeparationDefault`

### Main agent — function groups
- **Victim Placement**: `fnPlaceVictimsGMM`
- **Pheromone**: `fnGetPheromone(int idx)`, `fnDepositPheromone(int idx, double amount)`

---

## Known missing / incomplete features (as of 2026-05-09)

- **Battery depletion from movement**: `varMoveEnergyCost` is defined but no code subtracts it. `fnSenseNow` subtracts scan cost but flight distance is never billed.
- **Exploit state movement**: The statechart transitions to Exploit when a victim is detected, but the Exploit state has no entry action. Add `moveTo(varTargetX, varTargetY)` there.
- **`fnSenseNow` does not call `fnSenseNowGrid`**: Grid scanning and pheromone deposits only happen if `fnSenseNowGrid()` is called at the end of `fnSenseNow`. This call is currently missing.
- **Grid coverage**: The belief grid covers 800x400 px (20 cols x 10 rows x 40 px/cell, origin at 100,100). If the actual search space is larger, UAVs outside the grid will clamp to edge cells.
- **Experiment Defaults not wired up**: The `var*Default` variables in Main exist for Parameter Variation experiments but no startup code reads them and pushes values into UAV instances yet.
- **Pheromone heatmap**: All pheromone data exists in `varPheromoneGrid` but there is no visual overlay in the simulation view.

---

## Merge conflicts

Git merge conflicts in `_alp/**/*.xml` files are dangerous. The most common casualty is the `<Body xmlns:al="http://anylogic.com"/>` line being dropped during a three-way merge. After any merge touching these files, verify that every function that should have a body still has its `<Body/>` marker in the XML **and** its code block in the Java file.
