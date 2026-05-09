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
Stores function **metadata** (name, return type, ID, position). For functions that have a body, there must be a `<Body xmlns:al="http://anylogic.com"/>` element — this is a **presence marker** that tells AnyLogic the body exists in the Java file. Functions with no body have no `<Body>` element at all.

```xml
<Function AccessType="default" StaticFunction="false">
    <ReturnModificator>VOID</ReturnModificator>
    <ReturnType>void</ReturnType>
    <Id>1778000001001</Id>
    <Name><![CDATA[fnMoveToRandomWaypoint]]></Name>
    ...
    <Body xmlns:al="http://anylogic.com"/>   <!-- present = has body; absent = empty -->
</Function>
```

### `_alp/Agents/<Agent>/Code/Functions.java`
Stores the **actual function body code**, delimited by ID markers:

```java
void fnMoveToRandomWaypoint()
{/*ALCODESTART::1778000001001*/
// actual code here
/*ALCODEEND*/}
```

The ID in `ALCODESTART::ID` must match the `<Id>` in `Functions.xml`.
The Java file is the source of truth for code. The XML is the source of truth for metadata.

---

## CRITICAL: Line endings must be CRLF (`\r\n`)

AnyLogic writes all project files with **Windows CRLF (`\r\n`) line endings**.
If you write a `Functions.java` with LF-only (`\n`) endings, AnyLogic will detect the mismatch on next open and **regenerate the file from scratch**, producing empty function stubs and wiping all code.

**Never use a standard text-writing tool to write `.java` files in this project.**
Instead, use Python in binary mode to guarantee CRLF:

```python
lines = [
    'void myFunction()',
    '{/*ALCODESTART::1234567890*/',
    'double x = 1.0;',
    '/*ALCODEEND*/}',
    '',
]
with open(r'path\to\Functions.java', 'wb') as f:
    f.write(b'\r\n'.join(line.encode('utf-8') for line in lines) + b'\r\n')
```

You can verify line endings afterwards:
```python
with open(path, 'rb') as f:
    data = f.read()
assert data.count(b'\n') == data.count(b'\r\n'), "LF-only endings detected!"
```

---

## What goes wrong and why

| Mistake | What AnyLogic does |
|--------|-------------------|
| Write Java file with LF endings | Detects external modification, regenerates Java from XML (empty bodies) |
| Write Java file while AnyLogic is open | AnyLogic overwrites it from in-memory state on next save |
| Remove `<Body xmlns:al=.../>` from XML | AnyLogic shows empty body in UI, treats function as having no code |
| Add CDATA content to `<Body>` element | AnyLogic ignores it; code must be in the Java file, not the XML |

---

## Other model elements (variables, events, etc.)

Variables are stored in `_alp/Agents/<Agent>/Variables.xml` and follow the same pattern — metadata in XML. If they have initial value code it is stored inline in the XML as CDATA (not in a separate Java file). Only `Functions.java` uses the separate-file pattern with ALCODESTART markers.

---

## Merge conflicts

Git merge conflicts in `_alp/**/*.xml` files are dangerous. The most common casualty is the `<Body xmlns:al="http://anylogic.com"/>` line being dropped during a three-way merge. After any merge touching these files, verify that every function that should have a body still has its `<Body/>` marker in the XML **and** its code block in the Java file.
