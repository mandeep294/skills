# Design principles

The *why* behind the code-assessment standard. The step-by-step is in [`runbook.md`](runbook.md);
this captures the design decisions a contributor must not quietly undo when adding patterns.
Per-run conduct — surgical edits, branch/in-place, rollback, skip-with-reason — is stated as rules
in [`../SKILL.md`](../SKILL.md) and the runbook and is not repeated here.

## Warn, don't gate

Cheap checks run early, but they **warn** — they don't block. A dirty working tree or a failing
baseline `mvn compile` is recorded and surfaced, not treated as a stop. The only hard stops are real
safety issues: a verification failure after edits (triggers rollback) and an existing `autofix/*`
branch without user consent.

**Why:** customers routinely have WIP changes or an already-broken HEAD and still want a findings
report or a targeted fix. A run that exits before producing anything useful helps no one. Prefer
**warn + report + ask** over **stop + fix-your-environment-first**.

## Detection and remediation are separable

A pattern has two distinct jobs — **detection** (find instances) and **remediation** (fix them) —
that communicate only through the **findings shape** (`pattern`, `file`, `line`, `snippet`). The
expert skill's Discovery section owns detection; its recipe owns remediation.

**Why:** a clean seam lets either side change without disturbing the other — most importantly, the
*detection method* can be upgraded without rewriting the fix. A recipe must never assume how its
findings were produced.

## Determinism-preferred detection

LLM `scan` is the zero-setup default and the universal fallback — no toolchain, works anywhere. But
where a pattern is high-volume or precision-sensitive (LLM scanning would be slow, token-heavy, or
miss/over-flag), a **deterministic detector is the preferred end-state**.

**Why:** deterministic detection is reproducible and scales; LLM scanning does not. So detection
method is a **per-pattern choice** in the catalog (`scan` | `analyzer`), not a global stance. The
`analyzer` value and [`../scripts/`](../scripts/README.md) are reserved for that future; because
detection and remediation are separable, the upgrade touches neither the recipe nor the runbook.

## One pattern per session

A report may span every pattern found, but **applying** fixes touches exactly one pattern per
session; the rest go to `deferred[]` for a follow-up run.

**Why** — four independent, load-bearing reasons:
- **Atomic revert** — one pattern's edits revert as a coherent set; mixing two makes `git restore` unsafe to reason about.
- **Coherent review** — a diff that tells one story is far faster to review than a mixed-purpose one.
- **Recipe isolation** — each pattern's recipe has its own decision table and skip policy; one at a time keeps attention focused.
- **Bounded context** — combined pattern outputs can blow past the model's effective context.

"Fix everything" is therefore refused for the apply phase: list the patterns, ask the user to pick one.

## Scope discipline

Edit **only** what the source named: the user's paths/coordinates, or what the recipe's antipattern
actually matched. Never opportunistically clean up nearby code.

**Why:** scope discipline is what makes the skill trustworthy. The moment it edits outside its stated
input, the customer can no longer review the diff without auditing the whole project.

---

## Where to read next

- [`../SKILL.md`](../SKILL.md) — control plane: routing + classification.
- [`runbook.md`](runbook.md) — the standard run procedure.
- [`git-workflow.md`](git-workflow.md) — run-log schema, branch / in-place mechanics.
- `../<pattern>/SKILL.md` + `recipe.md` — per-pattern expert skills.
