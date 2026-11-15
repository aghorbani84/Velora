package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class PerformanceInstrumentedTest {
    @Test
    fun captureColdStartMemoryAndCpuOnEmulator() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val launchIntent = Intent(targetContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val startNanos = SystemClock.elapsedRealtimeNanos()
        val activity = instrumentation.startActivitySync(launchIntent)
        val coldStartMs = (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000L
        assertTrue("MainActivity did not become visible within 15 seconds", coldStartMs < 15_000L)

        val pid = waitForTargetPid(targetContext, 10_000L)
        assertTrue("Could not resolve target process PID", pid > 0)
        SystemClock.sleep(5_000L)

        val activityManager = targetContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val beforeMemory = activityManager.getProcessMemoryInfo(intArrayOf(pid)).firstOrNull()
        val beforeCpuTicks = readCpuTicks(pid)
        val sampleStart = SystemClock.elapsedRealtime()
        SystemClock.sleep(5_000L)
        val afterMemory = activityManager.getProcessMemoryInfo(intArrayOf(pid)).firstOrNull()
        val afterCpuTicks = readCpuTicks(pid)
        val sampleDurationMs = max(1L, SystemClock.elapsedRealtime() - sampleStart)
        val cpuPercent = if (beforeCpuTicks != null && afterCpuTicks != null) {
            val cpuSeconds = (afterCpuTicks - beforeCpuTicks).toDouble() / 100.0
            cpuSeconds / (sampleDurationMs / 1_000.0) * 100.0
        } else {
            -1.0
        }

        val pssKb = afterMemory?.totalPss ?: beforeMemory?.totalPss ?: -1
        val metric = String.format(
            Locale.US,
            "METRIC version=%s device=%s api=%d cold_start_ms=%d pss_kb=%d cpu_percent_5s=%.2f sample_ms=%d pid=%d",
            BuildConfig.VERSION_NAME,
            android.os.Build.MODEL,
            android.os.Build.VERSION.SDK_INT,
            coldStartMs,
            pssKb,
            cpuPercent,
            sampleDurationMs,
            pid
        )
        Log.i(TAG, metric)
        assertTrue("PSS measurement was unavailable", pssKb > 0)
        activity.finishAndRemoveTask()
    }

    private fun waitForTargetPid(context: Context, timeoutMs: Long): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            val process = activityManager.runningAppProcesses
                ?.firstOrNull { it.processName == context.packageName }
            if (process != null && process.pid > 0) return process.pid
            SystemClock.sleep(250L)
        }
        return -1
    }

    private fun readCpuTicks(pid: Int): Long? {
        return runCatching {
            val stat = java.io.File("/proc/$pid/stat").readText()
            val fields = stat.substringAfterLast(") ").split(' ')
            fields[11].toLong() + fields[12].toLong()
        }.getOrNull()
    }

    private companion object {
        const val TAG = "VeloraPerf"
    }
}
