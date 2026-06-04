---
name: outdated-dependencies
description: "AEM Cloud Service expert skill — upgrade outdated Maven dependencies in pom.xml, both literal <version> and same-pom ${property} shapes. Use for \"update my aem-sdk-api\", \"upgrade mockito\", or scanning a project for stale dependency versions. Discovery can find <dependency> blocks but \"outdated\" needs a target version, which the user supplies. Pattern A/B locators and editing strategy are in recipe.md."
license: Apache-2.0
---

# Outdated Maven dependencies — AEM as a Cloud Service

> This pattern is executed by the code-assessment runbook — follow [`../references/runbook.md`](../references/runbook.md) for the full flow (preflight → plan → apply → verify, run log). This skill supplies the detection + recipe the runbook applies.

## Overview

Stale Maven dependencies (notably `aem-sdk-api`) cause build failures and local/runtime drift. This skill bumps a dependency's version surgically — literal `<version>` or a same-pom `${property}` — without reformatting the pom.

## Classification — confirm this pattern applies

- A `pom.xml` with a `<dependency>` whose version the user wants raised, either as a literal `<version>` or via a `<version>${prop}</version>` + `<properties>` entry.
- Not for `<plugin>` versions, `<dependencyManagement>` / BOM overrides, or parent-pom inheritance (not currently supported).

## Discovery (standalone)

**Partial.** The skill can enumerate `<dependency>` blocks under workspace roots, but "is this outdated?" and "what is the target?" require the user to say — see Resolution contract.

```bash
rg -l '<dependency>' --glob 'pom.xml' <workspace-root>   # or: grep -rl '<dependency>' --include='pom.xml' <workspace-root>
```

Discover-time check: for each candidate `(groupId, artifactId, currentVersion)`, count matching `<dependency>` blocks in the file; if > 1, record `discovery_warnings[]` and pre-skip with `ambiguous-locator`. Do **not** invent target versions.

## Resolution contract

**user-supplied** — list the found coordinates with their current versions and ask which to upgrade and to what target version before planning. Never guess a version.

## Review checklist

- [ ] Only the `<version>` text (or the `<properties>` entry) changed — no whitespace/attribute churn
- [ ] Property shape edits validated: property exists, value matched, referenced by the target dependency
- [ ] Ambiguous (multi-match) locators skipped, not guessed
- [ ] Target version came from the user — never invented

## Recipe

Read [`recipe.md`](recipe.md) in full before editing: input contract, Pattern A (literal), Pattern B (property), multi-module caveat, editing strategy.

## Handoff

The skill never commits. See [`../references/git-workflow.md`](../references/git-workflow.md) for git vs in-place handoff and the suggested commit message.
