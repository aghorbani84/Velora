# Velora — MANUAL PUSH (user action, ~5 minutes)

The improved Velora code lives on a local branch in this VM's clone and could
**not** be pushed by the agent: the GitHub App still lacks the Workflows
permission (probe 2026-11-13: `403 Resource not accessible by integration`) and
non-workflow pushes through the App are also not available for this branch.
**Nothing has been pushed, no PR opened, no release created.**

## What you get

- Branch `fix/velora-requirements-20261113` (based on `74d036a`), one commit:
  the 4 user requirements — background playback service start (B-VEL-04),
  immersive fullscreen + `FullscreenPolicy` + 4 unit tests (B-VEL-05),
  Simple Mode on the video screen (B-VEL-06). Req 2 (pro settings) was
  verified already working — no change.
- Patch file: `~/workspace/PROJECT_HQ/projects/velora/delivery/velora-requirements-20261113.patch`
  (ask Glimmer for it, or take it from the workspace).
- Hardened workflows (separate files, not in this patch):
  `~/workspace/PROJECT_HQ/projects/velora/hardening/` — `release.yml`,
  `android.yml`, `HARDENING_DIFF.md`, `release.diff`, `android.diff`.

## Steps (on your own machine, in your Velora clone)

```bash
cd <your Velora clone>
git checkout main && git pull
git checkout -b fix/velora-requirements-20261113
git apply velora-requirements-20261113.patch
git add -A && git commit -m "feat: 4 user requirements — background service, immersive fullscreen, Simple Mode on video"
git push -u origin fix/velora-requirements-20261113
```

Then open a PR (branch → main) from GitHub and watch CI go green. The new
test `com.example.FullscreenPolicyTest` is included; the CI test whitelist in
the hardened workflows already lists it.

## Optional: land the hardened workflows in the same branch

```bash
cp <hardening-folder>/release.yml .github/workflows/release.yml
cp <hardening-folder>/android.yml .github/workflows/android.yml
git add .github/workflows/release.yml .github/workflows/android.yml
git commit -m "ci: harden release + CI workflows (fail-fast signing, least privilege, pinned actions)"
git push
```

## Do NOT do yet — v2.3.1 re-release

1. **First** configure the REAL release signing secrets in the repo:
   Settings → Secrets and variables → Actions →
   `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`
   (the keystore behind them must be the long-term upload key, alias `upload`).
2. Only then push tag `v2.3.1` (or run the release workflow with dispatch,
   default tag is now `v2.3.1` in the hardened file).
3. Verify the new APK's signature fingerprint matches your real keystore
   before announcing it.

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
