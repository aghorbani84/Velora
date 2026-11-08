# ADR 0002: Application Identity (`applicationId`) & Namespace Retention Strategy

- **Status:** Accepted
- **Date:** 2026-11-08
- **Deciders:** Principal Android / Media Architecture Team
- **Related:** `app/build.gradle.kts`, `app/google-services.json`, `CONTRIBUTING.md`

---

## 1. Context & Problem Statement

During the transition from the early prototype codename ("Fazora") to **VELORA (*Beyond Playback*)**, two identity identifiers remained in `app/build.gradle.kts`:
1. `applicationId = "com.aistudio.fazora.audiosync"`
2. `namespace = "com.example"` (and source packages `com.example.{ui,orchestrator,domain,data}`)

We evaluated whether to perform a breaking rename of `applicationId` and `namespace` to `com.velora.app` in the `2.x` release line or retain the existing binary/package coordinates while standardizing all class, database, service, and artifact identities on `Velora*`.

---

## 2. Considered Options

| Criterion | Option A: Retain `applicationId` & `namespace` (Selected) | Option B: Breaking Migration to `com.velora.app` |
| :--- | :--- | :--- |
| **In-Place APK Update Compatibility** | **PASS:** Existing installs upgrade seamlessly (`INSTALL_REPLACE_EXISTING`) without uninstalling. | **FAIL:** Android PackageManager treats new `applicationId` as a separate app; requires full uninstall or side-by-side duplicate install. |
| **Room v3 & DataStore Preservation** | **PASS:** Preserves `/data/data/com.aistudio.fazora.audiosync/databases/velora_media.db` (playlists, custom EQ presets, playback history) and DataStore preferences. | **FAIL:** Destroys local offline library metadata, custom 10-band EQ curves, and session state unless manual export/import is performed. |
| **Firebase Auth & Firestore Provisioning** | **PASS:** Matches `app/google-services.json` (`package_name = "com.aistudio.fazora.audiosync"`, OAuth client `999217457240-62re3kkq8697urct0693ubaij3a7ku2v`). | **FAIL:** Breaks `processDebugGoogleServices` / `processReleaseGoogleServices` and Google Sign-In (`CredentialManager`) until a new Firebase Android client is provisioned. |
| **AI Studio Build Container & `CONTRIBUTING.md`** | **PASS:** Complies with AI Studio container `namespace = "com.example"` invariant and `CONTRIBUTING.md` Section 1.2 layer boundaries. | **FAIL:** Violates AI Studio streaming emulator `namespace` rules and breaks existing deep-link / test runner scripts. |

---

## 3. Decision

1. **Retain `applicationId = "com.aistudio.fazora.audiosync"`** throughout the `2.x` release series to guarantee zero-data-loss over-the-air APK updates and uninterrupted Firebase Authentication / Firestore synchronization.
2. **Retain `namespace = "com.example"`** and the four documented Clean Architecture sub-packages (`com.example.ui`, `com.example.orchestrator`, `com.example.domain`, `com.example.data`) as codified in `CONTRIBUTING.md`.
3. **Enforce `Velora*` Canonical Naming** on all classes, services (`VeloraPlaybackService`), application entrypoints (`VeloraApplication`), databases (`VeloraDatabase`), Koin modules (`VeloraModule`), and release binaries (`VELORA-v<VERSION>-release-signed.apk`).
4. **Single Version Source of Truth (`VERSION` & `gradle.properties`):** Eliminate version drift between `app/build.gradle.kts`, `README.md`, `CHANGELOG.md`, and `.github/workflows/release.yml` by reading the canonical semantic version (`2.3.0`, `versionCode = 6`) from root `VERSION` / `gradle.properties`.

---

## 4. Consequences

- **Positive:** Users can install every new `VELORA-v2.3.0+` APK directly over previous builds without losing playlists, EQ presets, or offline history; CI and Google Services builds remain 100% deterministic.
- **Negative / Mitigation:** Developers inspecting `app/build.gradle.kts` see `com.aistudio.fazora.audiosync`; an inline comment referencing this ADR has been added to `app/build.gradle.kts` so the rationale is immediately visible.
