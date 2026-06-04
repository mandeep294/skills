---
name: code-assessment
description: |
  Detect and fix AEM as a Cloud Service code-quality issues locally, with no external
  services or network calls. Each issue type is handled by a self-contained expert skill
  under this skill. Name the files to fix, or ask it to scan the repo and find issues
  itself; it then plans, applies surgical edits on a branch (or in place), and verifies
  with mvn compile. Use whenever a user wants to detect, review, or remediate AEM
  Java / Maven / Sling code-quality issues in a project.
license: Apache-2.0
---

# AEM as a Cloud Service — Code Assessment

Single skill for detecting and fixing AEM CS code-quality issues, **entirely against the local workspace** — no external services or network calls. Findings reach the runbook from one of two sources; everything downstream is identical.

## Findings sources

| Source | When | Target versions (deps) |
|---|---|---|
| **User-named** | the user names files or coordinates | user-supplied |
| **Discover** | the user asks to scan, or names no files | user-supplied (per the pattern's resolution contract) |

Per pattern, discovery uses an LLM `scan` today; a deterministic `analyzer` is a reserved future option that emits the same findings shape — see [`references/patterns.md`](references/patterns.md) and [`scripts/`](scripts/README.md).

## Routing

1. **User named files / coordinates** → run the runbook in `with_findings` mode against those paths.
2. **"Scan my repo" / no files named** → run the runbook in `discover` mode (per-pattern Discovery, workspace roots only).

Then follow the runbook: [`references/runbook.md`](references/runbook.md).

## Manual Pattern Hints (classification → expert skill)

Route the request to one expert skill:

| User said / saw | Expert skill |
|---|---|
| "update aem sdk", "upgrade mockito", stale `<version>` or `${property}` in pom | [`outdated-dependencies/`](outdated-dependencies/SKILL.md) |
| "fix @Inject", "modernize Sling Models", `javax.inject.Inject` on `@Model` fields | [`inject-in-sling-model/`](inject-in-sling-model/SKILL.md) |

If nothing matches, say the issue is not yet supported and offer to file a request for a new expert skill.

**Full catalog** (built + `planned` patterns, with severity / detection / fix): [`references/patterns.md`](references/patterns.md).

## Runbook

All detection, planning, edits, verification, git/in-place handling, and the run log live in
[`references/runbook.md`](references/runbook.md). The runbook is the **sole owner** of repo-environment
detection (`edit_mode`, git snapshot) — this control plane does not duplicate it.

## One pattern per session

Report may span every pattern found; **apply touches one pattern per session** (atomic revert,
single-story diff). Refuse "fix everything" for the apply phase. Rationale:
[`references/shared-principles.md`](references/shared-principles.md#one-pattern-per-session).

## Critical rules

- **Local only** — no network calls or external services; operate solely on the workspace.
- **Never commit, push, or open a PR** — branch (git) or in-place edits only; the customer reviews and commits.
- **Surgical edits** — no reformatting / re-serialization.
- **Skip with a reason** — record un-applicable findings as `skipped` with an exact reason; never silently drop.
- **One pattern per session** for apply.

Full rationale: [`references/shared-principles.md`](references/shared-principles.md).

## Scope & limitations

Local static detection and remediation only — no external services, no network, no live AEM instance. Issues that require runtime or live-repository state, telemetry, or history across runs are out of scope for this skill.

## Adding a new pattern

Copy [`references/_template.md`](references/_template.md) to `code-assessment/<pattern-name>/`,
add a row to [`references/patterns.md`](references/patterns.md) and to **Manual Pattern Hints**
above, then run `npm run validate`.

## Related skills

- **`migration`** — legacy AEM-version migration.
- **`best-practices`** — general AEM CS Java/OSGi patterns.
