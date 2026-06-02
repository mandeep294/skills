---
name: browser-probe
license: Apache-2.0
compatibility: Requires playwright-cli on PATH. Run `playwright-cli --help` for usage.
description: >-
  Probe a URL with escalating headless browser configurations to detect CDN bot
  protection (Akamai, Cloudflare, DataDome, AWS WAF) and produce a
  browser-recipe.json that downstream playwright-cli consumers use to bypass
  blocking. Runs an automated escalation ladder: default headless → stealth
  script injection → system Chrome (TLS fingerprint fix) → persistent profile.
  Use BEFORE any playwright-cli interaction with an untrusted domain. Triggers
  on: browser probe, site blocked, headless blocked, CDN blocking, bot
  detection, browser recipe, can't load page, 403 error page, access denied.
---

# Browser Probe

Detect CDN bot protection blocking headless Chrome and produce a browser recipe
for downstream `playwright-cli` consumers. Node 22+ required. No npm
dependencies.

## When to Use

Run this skill **before** any `playwright-cli` interaction with a domain you
haven't tested, or when a downstream script reports a blocked page. Common
triggers:

- First interaction with a new domain
- `capture-snapshot.js` produces empty/error snapshots
- Page title contains "error", "denied", "blocked", "captcha"
- HTTP 403 responses from headless browser

## Script Location

```bash
if [[ -n "${CLAUDE_SKILL_DIR:-}" ]]; then
  PROBE_DIR="${CLAUDE_SKILL_DIR}/scripts"
else
  PROBE_DIR="$(dirname "$(command -v browser-probe.js 2>/dev/null || \
    find ~/.claude -path "*/browser-probe/scripts/browser-probe.js" \
    -type f 2>/dev/null | head -1)")"
fi
```

## Workflow

### Step 1 — Run the probe

```bash
node "$PROBE_DIR/browser-probe.js" "$URL" "$OUTPUT_DIR"
```

The script tries up to 5 browser configurations, stopping at the first success:

1. **default** — headless Chromium (baseline)
2. **stealth** — headless Chromium + JS stealth init script (patches `navigator.webdriver`, plugins, languages)
3. **stealth-ua** — headless Chromium + JS stealth + User-Agent override (removes `HeadlessChrome` from HTTP UA header via `--user-agent` launch arg)
4. **chrome** — system Chrome (`--browser=chrome`) + JS stealth + UA override (fixes TLS fingerprint detection)
5. **persistent** — system Chrome + JS stealth + UA override + persistent profile (cookie/session challenges)

Output: `$OUTPUT_DIR/probe-report.json`

### Step 2 — Read the report

Load `probe-report.json`. Check `firstSuccess`:
- If non-null: a configuration worked. Proceed to Step 3.
- If null: all configurations failed. Skip to Step 5.

### Step 3 — Interpret results

Load the stealth configuration reference at `references/stealth-config.md` and match the
`detectedSignals` array against the Provider Signature Table.

| Signal(s) | Interpretation | Minimum config |
|-----------|----------------|----------------|
| `cloudfront-block` or stealth fails / stealth-ua succeeds | CloudFront UA blocking (`HeadlessChrome` in HTTP header) | `stealth-ua` |
| `cloudfront` (no `cloudfront-block`) | CloudFront present, not blocking | default |
| `akamai-server` / `akamai-bot-manager` | TLS fingerprint blocking | `chrome` (stealth+UA alone insufficient) |
| `cloudflare-ray` (no `cloudflare-challenge`) | Cloudflare present, not blocking | default |
| `cloudflare-challenge` | Active JS challenge | `chrome` + stealth + UA |
| `datadome` | Aggressive detection | `chrome` + stealth + UA |
| `aws-waf` | Usually UA-based | `stealth-ua` |
| no signals + blocked | Unknown protection | `persistent` (last resort) |

### Step 4 — Generate recipe

Write `browser-recipe.json` to `$OUTPUT_DIR`:

```json
{
  "url": "<probed URL>",
  "generated": "<ISO timestamp>",
  "cliConfig": {
    "browser": {
      "browserName": "chromium",
      "launchOptions": { "channel": "<from firstSuccess step>" }
    }
  },
  "stealthInitScript": "<full script from stealth-config.md if stealth was needed>",
  "notes": "<1-2 sentence explanation of what was detected and why this config>"
}
```

**Config mapping from `firstSuccess`:**

| firstSuccess | cliConfig.launchOptions | stealthInitScript |
|---|---|---|
| `default` | `{}` (no channel, no args) | `null` (not needed) |
| `stealth` | `{}` (no channel, no args) | Full stealth script from reference |
| `stealth-ua` | `{ "args": ["--user-agent=<realistic UA>"] }` | Full stealth script from reference |
| `chrome` | `{ "channel": "chrome", "args": ["--user-agent=<realistic UA>"] }` | Full stealth script from reference |
| `persistent` | `{ "channel": "chrome", "args": ["--user-agent=<realistic UA>"] }` | Full stealth script from reference |

If `firstSuccess` is `persistent`, add a `"persistent": true` field to the
recipe so consumers know to use `--persistent`.

### Step 5 — Report results

**If a configuration worked:**
```
Browser probe complete for <url>.
  Working config: <firstSuccess>
  Detected: <detectedSignals or "no bot protection detected">
  Recipe: <path to browser-recipe.json>
```

**If all configurations failed:**
```
Browser probe failed for <url>. No headless configuration could load the page.
  Tried: default, stealth, stealth-ua, chrome, persistent
  Detected signals: <detectedSignals>

  Options:
  1. Use --headed flag for manual browser interaction
  2. Provide pre-captured data (DOM snapshot, screenshots) manually
  3. Check if the URL requires authentication or VPN access
```

Do NOT produce a recipe when all steps fail. Do NOT silently continue
with a broken configuration.

## How Consumers Use the Recipe

Pass `--config=<path-to-cliConfig>` to `playwright-cli open`. If the recipe has
`stealthInitScript`, add it to `browser.initScript` in the config (not via `eval` —
eval is expression-only). If `"persistent": true`, also pass `--persistent`.
Run `playwright-cli --help` for the full command reference.
