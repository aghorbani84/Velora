# Velora — MANUAL PUSH (user action, ~5 minutes)

## Current state (2026-11-13, verified)

- GitHub branch **`fix/velora-requirements-20261113` EXISTS** — pushed by the
  agent via the GitHub contents API, commit `f5b67f4`, based on current main
  `e571126a` (the user's 2026-11-13 DSP commit, not the old `74d036a`):
  https://github.com/aghorbani84/Velora/tree/fix/velora-requirements-20261113
- That branch contains the 4-requirements fix (background service start
  B-VEL-04, immersive fullscreen + `FullscreenPolicy` + tests B-VEL-05,
  Simple Mode on video B-VEL-06; pro settings verified already working).
- **NOT on GitHub yet: the hardened workflows** (fail-fast signing fix,
  B-VEL-03). The contents API returns `403 Resource not accessible by
  integration` for any tree containing `.github/workflows/` — the App still
  lacks the Workflows permission. The workflow fix lives in the local branch
  (commit `e2e4363`) and in `velora-signing-20261113.patch` in this folder.
- **No PR opened, no release created, no APK published.**
- Codeberg mirror repo exists but is **still empty**:
  https://codeberg.org/aghorbani84/Velora

## Step 1 — Land the signing fix on the GitHub branch (your machine)

```bash
cd <your Velora clone>
git fetch origin
git checkout fix/velora-requirements-20261113
git apply velora-signing-20261113.patch   # from this delivery folder
git add -A && git commit -m "fix: fail-fast release signing, hardened workflows (B-VEL-03)"
git push
```

(Alternative: `git fetch` the local branch is not available — the patch is
the transport. The full two-commit patch `velora-requirements-20261113.patch`
is also in this folder if you prefer to build the branch from scratch on
current main `e571126a`.)

Then open a PR (branch → main) from GitHub and watch CI go green. The new
test `com.example.FullscreenPolicyTest` is included; the CI test whitelist in
the hardened workflows already lists it.

## Step 2 — Mirror to Codeberg (your machine)

```bash
git remote add codeberg https://codeberg.org/aghorbani84/Velora.git
git push codeberg fix/velora-requirements-20261113 --tags
git push codeberg main --tags
```

Codeberg has no GitHub Actions; it is a code mirror for users in Iran
(GitLab/Bitbucket/SourceForge deny service to Iran — not used).

## Do NOT do yet — v2.3.1 re-release

1. **First** generate a REAL release keystore on your own machine and add
   the three secrets — full steps in
   `~/workspace/PROJECT_HQ/projects/velora/SIGNING_SETUP.md`:
   `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`
   (key alias must be `upload`).
2. Only then push tag `v2.3.1` (or run the release workflow with dispatch,
   default tag is `v2.3.1` in the hardened file).
3. Verify the new APK's signature fingerprint matches your real keystore
   before announcing it (steps in SIGNING_SETUP.md).

**Why:** v2.3.0 (2026-11-12) was signed with a one-time ephemeral key whose
private key is gone (B-VEL-03). Anyone on v2.3.0 cannot update to a real-key
build — they will need a reinstall. Never sign a release with an ephemeral
key again; the hardened workflow fails the run instead.

## Also still open (user-only)

- GitHub App Workflows permission: Settings → GitHub Apps → Muse →
  Workflows = Read and write AND Velora in the repo list (or reinstall:
  https://github.com/apps/meta-muse-ai/installations/new).
- Rotate the exposed secrets from the 2026-11-10 survey (public
  `railway-hermes-backup` PAT / Telegram token / API key) — privatising the
  repo alone is not enough.
- Gitee: STOPPED by the user 2026-11-13 — no Gitee work.
