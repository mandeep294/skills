# scripts/ — reserved for deterministic detection (future)

Standard home for deterministic detection / parsing scripts. **Reserved — nothing here yet.**

Today, detection is LLM-native (the agent scans the workspace) or user-supplied. Where a
pattern is high-volume or precision-sensitive, a deterministic local detector is the
preferred end-state — see "determinism-preferred detection" in
[`../references/shared-principles.md`](../references/shared-principles.md).

A detector added here would, conceptually:

```
read the workspace → emit findings in the standard shape:
    { pattern, file, line, snippet }
…consumed by the runbook exactly like LLM-scan findings
(see the findings shape in ../references/runbook.md).
```

Language and implementation are intentionally undecided. **Not yet implemented** — this
README documents the convention and keeps the reserved location visible. *(Illustrative
only; no stub code.)*
