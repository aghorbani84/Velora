package com.example

import com.example.domain.model.DeclarativePluginManifest
import com.example.domain.model.VeloraCompatibilityMatrix
import com.example.domain.model.VeloraDownloadValidator
import com.example.domain.model.VeloraPluginRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WP-5.1 & WP-7.1 Security verification suite for stream URL sanitization,
 * IDN homograph protection, IPv6 loopback/private rejection, HLS/DASH manifest guards,
 * and Tier-1 plugin sandbox capability enforcement.
 */
class DownloadValidatorSecurityTest {

    @Test
    fun `accepts legitimate HTTPS and HLS or DASH stream URLs`() {
        assertTrue(VeloraDownloadValidator.isValidStreamOrManifestUrl("https://stream.radioparadise.com/flac"))
        assertTrue(VeloraDownloadValidator.isValidStreamOrManifestUrl("https://cdn.velora.org/live/master.m3u8"))
        assertTrue(VeloraDownloadValidator.isValidStreamOrManifestUrl("https://cdn.velora.org/dash/manifest.mpd"))
    }

    @Test
    fun `rejects IPv4 loopback, RFC-1918 private networks, and cloud metadata endpoints`() {
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://127.0.0.1:8080/stream.mp3"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://localhost/live.m3u8"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://0.0.0.0:9000/audio.ogg"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://10.0.0.42/internal.mp3"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://172.16.0.1/stream.aac"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://172.31.255.254/stream.aac"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://192.168.1.100/home.mp4"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://169.254.169.254/latest/meta-data/"))
    }

    @Test
    fun `rejects IPv6 loopback, unique-local fc00, and link-local fe80 addresses`() {
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://[::1]:8080/live.m3u8"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("https://[0:0:0:0:0:0:0:1]/stream.mpd"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://[fc00::1]/internal.m3u8"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://[fd12:3456:789a::1]/audio.flac"))
        assertFalse(VeloraDownloadValidator.isValidStreamOrManifestUrl("http://[fe80::1ff:fe23:4567:890a]/live.ts"))
    }

    @Test
    fun `rejects IDN homograph and punycode spoofing URLs and redirect-based loopback targets`() {
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("https://xn--80ak6aa92e.com/stream.m3u8"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("https://радио.example.com/stream.mp3"))
        assertFalse(
            VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(
                "https://cdn.velora.org/proxy.m3u8?redirect=http://127.0.0.1:8080/admin"
            )
        )
        assertFalse(
            VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(
                "https://cdn.velora.org/proxy.mpd?url=file:///data/data/com.aistudio.fazora.audiosync"
            )
        )
    }

    @Test
    fun `validates HLS m3u8 and DASH mpd manifest payloads against embedded unsafe segments`() {
        val cleanM3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXTINF:10.0,
            https://cdn.velora.org/segments/seg01.ts
            #EXTINF:10.0,
            https://cdn.velora.org/segments/seg02.ts
        """.trimIndent()
        assertTrue(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(cleanM3u8))

        val poisonedM3u8File = """
            #EXTM3U
            #EXTINF:10.0,
            file:///etc/hosts
        """.trimIndent()
        assertFalse(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(poisonedM3u8File))

        val poisonedM3u8Loopback = """
            #EXTM3U
            #EXTINF:10.0,
            http://[::1]:8080/internal_seg.ts
        """.trimIndent()
        assertFalse(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(poisonedM3u8Loopback))
    }

    @Test
    fun `enforces Tier-1 plugin sandbox capability allowlist and signature verification`() {
        VeloraPluginRegistry.DEFAULT_DECLARATIVE_PLUGINS.forEach { plugin ->
            assertTrue(
                "Built-in plugin ${plugin.id} must pass sandbox verification",
                VeloraPluginRegistry.isPluginAllowedInSandbox(plugin)
            )
        }

        val capabilityEscalationPlugin = DeclarativePluginManifest(
            id = "malicious.plugin.dexloader",
            name = "Dynamic Code Loader",
            version = "1.0.0",
            publisher = "Untrusted",
            tier = "Tier 1",
            capabilities = listOf("theme.palette.saffron", "dex.dynamic_code_load"),
            verifiedSignature = true,
            isEnabled = true
        )
        assertFalse(VeloraPluginRegistry.isPluginAllowedInSandbox(capabilityEscalationPlugin))

        val unsignedPlugin = VeloraPluginRegistry.DEFAULT_DECLARATIVE_PLUGINS.first().copy(
            verifiedSignature = false
        )
        assertFalse(VeloraPluginRegistry.isPluginAllowedInSandbox(unsignedPlugin))
    }

    @Test
    fun `rejects authority userinfo confusion, IPv4-mapped IPv6, and decimal-octal-hex loopback bypasses (F1)`() {
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("https://public.com@127.0.0.1/stream.m3u8"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("https://user:pass@stream.radioparadise.com/flac"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://[::ffff:127.0.0.1]/stream.m3u8"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://[::ffff:10.0.0.1]/stream.m3u8"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://[::ffff:169.254.169.254]/meta"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://2130706433/stream.mp3"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://0177.0.0.1/stream.mp3"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://0x7f.0.0.1/stream.mp3"))
        assertFalse(VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl("http://127.1/stream.mp3"))
    }

    @Test
    fun `rejects HLS EXT-X-KEY URI and DASH BaseURL or SegmentTemplate SSRF payloads (F15)`() {
        val poisonedExtXKey = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-KEY:METHOD=AES-128,URI="http://127.0.0.1:8080/secret.key"
            #EXTINF:10.0,
            https://cdn.velora.org/segments/seg01.ts
        """.trimIndent()
        assertFalse(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(poisonedExtXKey))

        val poisonedExtXMap = """
            #EXTM3U
            #EXT-X-MAP:URI="file:///data/data/com.aistudio.fazora.audiosync/shared_prefs/secret.xml"
            #EXTINF:10.0,
            seg01.m4s
        """.trimIndent()
        assertFalse(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(poisonedExtXMap))

        val poisonedDashMpd = """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011">
              <Period>
                <AdaptationSet>
                  <BaseURL>http://169.254.169.254/latest/meta-data/</BaseURL>
                  <SegmentTemplate media="seg_${'$'}Number$.m4s" initialization="http://10.0.0.1/init.mp4" />
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
        assertFalse(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(poisonedDashMpd))

        val cleanDashMpd = """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011">
              <Period>
                <AdaptationSet>
                  <BaseURL>https://cdn.velora.org/dash/</BaseURL>
                  <SegmentTemplate media="seg_${'$'}Number$.m4s" initialization="init.mp4" />
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
        assertTrue(VeloraDownloadValidator.isSafeAdaptiveManifestPayload(cleanDashMpd))
    }

    @Test
    fun `rejects dot and dot-dot download filenames and enforces strict folder allowlist (N11, N16, N1)`() {
        assertFalse(VeloraDownloadValidator.isSafeDownloadTargetFileName(".."))
        assertFalse(VeloraDownloadValidator.isSafeDownloadTargetFileName("."))
        assertFalse(VeloraDownloadValidator.isSafeDownloadTargetFileName("sub/../escape.mp3"))
        assertTrue(VeloraDownloadValidator.isSafeDownloadTargetFileName("saffron_mix.flac"))

        // Strict allowlist in PhiDesignMetrics.isSafeMediaFolder (N16)
        assertTrue(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("/storage/emulated/0/Music"))
        assertTrue(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("/storage/emulated/0/Movies/Cinema"))
        assertTrue(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("content://com.android.externalstorage.documents/tree/primary%3AMusic"))
        assertFalse(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("/proc/self"))
        assertFalse(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("/sys/class"))
        assertFalse(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("/data/data/com.other.app"))
        assertFalse(com.example.domain.model.PhiDesignMetrics.isSafeMediaFolder("/storage/emulated/0/Music/../../etc"))

        // MediaSession URI validation (N1)
        assertTrue(
            com.example.data.playback.VeloraPlaybackService.isAllowedSessionMediaUri(
                "https://stream.radioparadise.com/aac-320"
            )
        )
        assertFalse(
            com.example.data.playback.VeloraPlaybackService.isAllowedSessionMediaUri(
                "http://127.0.0.1:8080/internal.mp3"
            )
        )
        assertFalse(
            com.example.data.playback.VeloraPlaybackService.isAllowedSessionMediaUri(
                "file:///data/data/com.aistudio.fazora.audiosync/databases/velora_library.db"
            )
        )
    }
}
