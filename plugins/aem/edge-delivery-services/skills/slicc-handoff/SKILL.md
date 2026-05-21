---
name: slicc-handoff
description: Hand off the current task to the SLICC browser agent, or install a new skill into SLICC from a GitHub repo. Use this skill when the user says things like "handoff to slicc", "move this to slicc", "move to the browser", "test in the browser", "handoff to browser", "install this skill in slicc", "upskill slicc with this repo", "add this skill to slicc", or otherwise asks you to continue the work inside the SLICC browser agent.
license: Apache-2.0
metadata:
  version: "1.0.0"
---

# SLICC Handoff

This skill bridges the current coding agent and the [SLICC](https://www.sliccy.ai) browser agent. It builds and dispatches a structured handoff that SLICC surfaces to the user as a yes/no approval card.

It supports two verbs:

- `handoff:<instruction>` — continue the current task inside SLICC.
- `upskill:<github-url>` — ask SLICC to install a skill from a GitHub repository.

## When to Use This Skill

Use this skill when the user wants to:

- Continue the in-progress task inside the SLICC browser agent (for example, to drive a real browser session, test a flow on a live site, or use a tool only available in SLICC).
- Install a skill from a GitHub repo into a running SLICC instance.

Do **not** use this skill when:

- The user just wants the local agent to keep working — no handoff is needed.
- The user wants to install a skill into the local agent (use the local agent's own skill install flow instead).

## What to Do

1. Compose a single-line, action-oriented instruction that captures what SLICC should do next. Include enough context for SLICC to pick up the work without your conversation history.
2. Pick a verb prefix:
   - `handoff:<instruction>` to continue a task.
   - `upskill:<github-url>` to install a skill. The URL can be the repo root or a `tree/<branch>/<sub/path>` URL to install only a sub-path of the repo.
3. Run the helper script with `--open`:

   ```bash
   node .claude/skills/slicc-handoff/scripts/slicc-handoff --open "handoff:<instruction>"
   ```

   ```bash
   node .claude/skills/slicc-handoff/scripts/slicc-handoff --open "upskill:<github-url>"
   ```

4. Tell the user that SLICC will show an approval prompt in its Chat tab, and they should accept it to dispatch the handoff.

If the user runs a non-default SLICC instance on another port, prefix the call with `SLICC_PORT=5720` (or whatever port they chose) so the localhost POST hits the right instance:

```bash
SLICC_PORT=5720 node .claude/skills/slicc-handoff/scripts/slicc-handoff --open "handoff:<instruction>"
```

## How It Works

The script builds a URL of the form `https://www.sliccy.ai/handoff?handoff=<urlencoded>` (or `?upskill=<urlencoded-github-url>`) and dispatches it through two parallel paths so the handoff reaches SLICC regardless of which Chrome profile holds the extension:

- **Localhost POST** to `http://localhost:${SLICC_PORT ?? 5710}/api/handoff` with a structured payload:

  ```json
  {
    "verb": "handoff" | "upskill",
    "target": "<github-url-for-upskill-or-handoff-url-for-handoff>",
    "instruction": "<prose-for-handoff-only>",
    "url": "<https-handoff-url>",
    "title": "SLICC handoff"
  }
  ```

  The SLICC node-server rebroadcasts the payload to the connected webapp as a `navigate` lick. This path is profile-independent — it reaches SLICC even when the user's default browser is a different Chrome profile than the one SLICC controls.

- **`--open`** opens the URL in the local browser. If that browser profile has the SLICC extension installed, `chrome.webRequest` parses the response's RFC 8288 `Link` header (rel `https://www.sliccy.ai/rel/handoff` or `https://www.sliccy.ai/rel/upskill`) and emits the navigate lick when one of those rels is present.

Either path results in a yes/no approval card in the SLICC cone; accepting it dispatches the handoff or upskill by verb prefix.

## Examples

Continue an in-progress signup flow in the browser:

```bash
node .claude/skills/slicc-handoff/scripts/slicc-handoff --open "Continue the signup flow in the browser"
```

Install a skill collection from a GitHub repo:

```bash
node .claude/skills/slicc-handoff/scripts/slicc-handoff --open "upskill:https://github.com/slicc/skills-extra"
```

Install only a single skill (a sub-path of a repo on a specific branch):

```bash
node .claude/skills/slicc-handoff/scripts/slicc-handoff --open "upskill:https://github.com/slicc/skills-extra/tree/main/skills/foo"
```

## Prerequisites

- Node.js 18+ installed (the script uses the global `fetch` and `AbortSignal.timeout` APIs).
- A browser opener available on the host (`open` on macOS, `xdg-open` on Linux, `cmd /c start` on Windows).
- A SLICC instance reachable either on `localhost:${SLICC_PORT ?? 5710}` (CLI/Electron float) or via the SLICC extension installed in the browser profile that opens the URL.

## Notes

- Treat the user-supplied instruction string as untrusted input that will be displayed to the user in SLICC for approval — do **not** embed secrets or credentials in it.
- The script intentionally swallows errors from the localhost POST so it still falls through to `--open` when no local SLICC server is running.
