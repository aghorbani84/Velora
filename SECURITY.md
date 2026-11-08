# Security Policy — VELORA (*Beyond Playback.*)

Security and user privacy are foundational engineering requirements in **VELORA**. Every subsystem is designed under a **Privacy-by-Default** and **Capability-Sandboxed** threat model.

---

## 1. Supported Versions

| Version | Supported | Security Updates |
| :--- | :--- | :--- |
| **`2.3.x` (VELORA)** | ✅ Yes | Active security, stream validation & DSP patches |
| **`2.0.x – 2.2.x`** | ✅ Yes | Security & stability backports |
| **`1.0.x` (Legacy)** | ⚠️ Critical fixes only | Upgrade to `2.3.0+` recommended |

---

## 2. Security Architecture & Built-in Defenses

### 2.1 Filesystem & Path-Traversal Isolation
- **Strict Media Path Allowlist (`PhiDesignMetrics.isSafeMediaFolder`):** Local scanning and user-selected folder paths are canonicalized and validated against traversal tokens (`..`, null bytes, symbolic links to restricted system roots such as `/proc`, `/sys`, `/data/data`, `/etc`).
- **Zero Broad Storage Permissions:** Complies strictly with Google Play policies by using scoped `MediaStore` queries (`READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` on API 33+) and the Storage Access Framework (`OpenDocumentTree`).

### 2.2 Secure Stream Validation (`VeloraDownloadValidator` & `VeloraCompatibilityMatrix`)
- **Secure Stream Validation (HTTPS/HLS/DASH URL sanitization):** Rejects untrusted IPv4 loopback (`127.0.0.1`, `localhost`, `0.0.0.0`), link-local (`169.254.x.x`), RFC-1918 private network addresses (`10.x.x.x`, `192.168.x.x`, `172.16–31.x.x`), and IPv6 loopback/unique-local/link-local addresses (`[::1]`, `fc00::/7`, `fe80::/10`) before passing URIs to ExoPlayer.
- **IDN-Homograph & Open-Redirect Sanitization:** Rejects non-ASCII and punycode (`xn--`) host spoofing as well as embedded redirect query parameters targeting internal or file URIs.
- **Adaptive Manifest Segment Inspection (`isSafeAdaptiveManifestPayload`):** Verifies `.m3u8` (HLS) and `.mpd` (MPEG-DASH) manifest payloads to ensure segment entries do not reference `file://`, `javascript:`, path traversal (`../`), or non-allowlisted loopback/private hosts.
- **Protocol Allowlist:** Permits only validated `https://` (and explicit user-initiated `http://` media streams) plus syntactically verified 40-hex / 32-base32 InfoHash `magnet:?xt=urn:btih:` links for authorized content.

### 2.3 Sandboxed Plugin Threat Model (`VeloraPluginRegistry`)
- **Threat Model:** Third-party media player extensions historically introduce three high-severity attack vectors:
  1. *Dynamic Code Execution (`.dex` / `.jar` / `.so` injection)* leading to arbitrary code execution within the app UID.
  2. *Capability Escalation* where a visual theme or lyrics adapter accesses private storage or exfiltrates listening history over the network.
  3. *Unsigned Manifest Tampering* where an unverified plugin impersonates a core component.
- **Mitigations (`VeloraPluginRegistry.isPluginAllowedInSandbox`):**
  - **Tier 1 Declarative-Only Execution:** Plugins are pure data manifests (`DeclarativePluginManifest`). No `DexClassLoader`, `PathClassLoader`, reflection, or JNI library loading is permitted anywhere in the runtime.
  - **Explicit Capability Allowlist (`ALLOWED_CAPABILITIES`):** Every plugin's requested capabilities are checked against the immutable allowlist (`theme.palette.saffron`, `wcag.contrast.guard`, `lyrics.sync.lrc`, `subtitle.rtl.persian`, `dsp.biquad.10band`, `visualizer.goertzel.rta`, `recommendations.local`, `privacy.zero_telemetry`). Any unrecognized capability (`dex.dynamic_code_load`, `network.raw`, `filesystem.write`) causes immediate rejection.
  - **Signature Gate:** `manifest.verifiedSignature` must be `true` before a plugin can be enabled.

### 2.4 Cloud Sync & Zero-Trust Firestore Rules (`firestore.rules`)
- All cloud synchronization is strictly opt-in behind **Google Sign-In (`CredentialManager`)**.
- Anonymous authentication is disabled. Every Firestore document under `/users/{userId}/...` enforces `request.auth != null && request.auth.uid == userId` alongside strict key allowlists (`hasAll` / `hasOnly`), field-length caps, and timestamp bounds (`<= request.time`).

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
