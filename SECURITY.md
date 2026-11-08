# Security Policy — VELORA (*Beyond Playback.*)

Security and user privacy are foundational engineering requirements in **VELORA**. Every subsystem is designed under a **Privacy-by-Default** and **Capability-Sandboxed** threat model.

---

## 1. Supported Versions

| Version | Supported | Security Updates |
| :--- | :--- | :--- |
| **`2.0.x` (VELORA)** | ✅ Yes | Active security & DSP patches |
| **`1.0.x` (Legacy)** | ⚠️ Critical fixes only | Upgrade to `2.0.0+` recommended |

---

## 2. Security Architecture & Built-in Defenses

### 2.1 Filesystem & Path-Traversal Isolation
- **Strict Media Path Allowlist (`PhiDesignMetrics.isSafeMediaFolder`):** Local scanning and user-selected folder paths are canonicalized and validated against traversal tokens (`..`, null bytes, symbolic links to restricted system roots such as `/proc`, `/sys`, `/data/data`, `/etc`).
- **Zero Broad Storage Permissions:** Complies strictly with Google Play policies by using scoped `MediaStore` queries (`READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` on API 33+) and the Storage Access Framework (`OpenDocumentTree`).

### 2.2 Secure Stream Validation (`VeloraDownloadValidator`)
- **Secure Stream Validation (HTTPS/HLS/DASH URL sanitization):** Rejects untrusted loopback (`127.0.0.1`, `localhost`, `::1`), link-local (`169.254.x.x`), and RFC-1918 private network addresses (`10.x.x.x`, `192.168.x.x`, `172.16–31.x.x`) in remote stream URLs before passing URIs to ExoPlayer.
- **Protocol Allowlist:** Permits only validated `https://` (and explicit user-initiated `http://` media streams) for `HLS (.m3u8)`, `MPEG-DASH (.mpd)`, and progressive media containers, plus syntactically verified 40-hex InfoHash `magnet:?xt=urn:btih:` links for authorized content.
- **No DRM Circumvention:** VELORA never bypasses DRM or unauthorized access controls.

### 2.3 Sandboxed Plugin Architecture (`VeloraPluginRegistry`)
- **Tier 1 Declarative Execution:** Plugins declare explicit capabilities (`THEME_CUSTOMIZATION`, `EQ_PRESET_PROVIDER`, `METADATA_ENRICHMENT`, `LYRICS_LOOKUP`, `SUBTITLE_FORMATTER`) and cannot execute arbitrary native binaries, reflection, or Dynamic Code Loading (`.dex` / `.jar` / `.so`).
- **Credential Isolation:** API keys and user tokens are never exposed to plugin contexts.

### 2.4 Cloud Sync & Zero-Trust Firestore Rules (`firestore.rules`)
- All cloud synchronization is strictly opt-in behind **Google Sign-In (`CredentialManager`)**.
- Anonymous authentication is disabled. Every Firestore document under `/users/{userId}/...` enforces `request.auth != null && request.auth.uid == userId` alongside schema and field-length validation.

---

## 3. Reporting a Vulnerability

If you discover a security vulnerability in VELORA:

1. **Do NOT open a public GitHub issue** containing exploit details.
2. Report the finding privately via GitHub's **Private Vulnerability Reporting** tab (**Security → Advisories → New draft security advisory**).
3. Include:
   - Affected component (`MediaOrchestrator`, `VeloraDownloadValidator`, `TenBandBiquadAudioProcessor`, `firestore.rules`, etc.).
   - Minimal reproduction steps or proof-of-concept unit test.
   - Potential impact assessment.

We aim to acknowledge security reports within **48 hours** and publish a verified fix and release binary within **7 days**.
