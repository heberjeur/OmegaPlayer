/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.util

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.StrictMode
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * In-app startup profiler. Records timestamped marks since process start, durations of
 * measured blocks, cumulative counters (e.g. thumbnail loading) and blocking events
 * (main-thread stalls detected by a watchdog, StrictMode disk/network violations on the
 * main thread). A readable report is written to the app's external files root a few
 * seconds after the first frame is drawn, and can also be generated on demand from the
 * Settings screen.
 */
object StartupTrace {

    const val REPORT_FILE_NAME = "startup-report.txt"
    private const val TAG = "StartupTrace"

    private const val SAMPLE_INTERVAL_MS = 32L
    private const val STALL_THRESHOLD_MS = 64L
    private const val WATCHDOG_WINDOW_MS = 60_000L
    private const val REPORT_DELAY_AFTER_FIRST_FRAME_MS = 5_000L
    private const val MAX_MARKS = 400
    private const val MAX_BLOCKERS = 60
    private const val MAX_STRICTMODE_EVENTS = 25
    private const val GAP_MIN_MS = 20L
    private const val MAX_GAPS = 12

    private data class Mark(
        val label: String,
        val atMs: Long,
        val mainThread: Boolean,
        val threadName: String,
        val durationMs: Long?
    )

    private data class Blocker(
        val atMs: Long,
        val kind: String,
        val headline: String,
        val detail: String,
        val busyMs: Long = 0L,
        val stackTop: String? = null
    )

    private val lock = Any()
    private val marks = ArrayList<Mark>()
    private val blockers = ArrayList<Blocker>()
    private val counters = LinkedHashMap<String, LongArray>()
    private val onceKeys = HashSet<String>()
    private var marksOverflow = false
    private var strictModeEvents = 0
    private var strictModeSuppressed = 0

    @Volatile private var processStartUptime = 0L
    @Volatile private var started = false
    @Volatile private var appContext: Context? = null
    @Volatile private var firstFrameAtMs: Long? = null
    @Volatile private var watchdogStarted = false
    @Volatile private var reportScheduled = false

    private val reportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _latestReport = MutableStateFlow<String?>(null)

    /** Latest generated report text, exposed for the in-app viewer. */
    val latestReport: StateFlow<String?> = _latestReport.asStateFlow()

    /** Must be the first call in Application.onCreate to anchor all timestamps. */
    fun start(application: Application) {
        synchronized(lock) {
            if (started) return
            started = true
            appContext = application.applicationContext
            processStartUptime = Process.getStartUptimeMillis()
        }
        mark("Application.onCreate begin")
    }

    // ---- Recording API -----------------------------------------------------------

    /** Records a point in time, on whichever thread calls it. */
    fun mark(label: String) {
        if (!started) return
        val entry = Mark(label, nowMs(), isMainThread(), Thread.currentThread().name, null)
        synchronized(lock) { addMarkLocked(entry) }
    }

    /** Records [label] only the first time [key] is seen (e.g. first data emission). */
    fun markOnce(key: String, label: () -> String) {
        if (!started) return
        synchronized(lock) { if (!onceKeys.add(key)) return }
        mark(label())
    }

    /** Runs [block], measures it and records the label together with its duration. */
    fun <T> trace(label: String, block: () -> T): T {
        val startedAt = SystemClock.uptimeMillis()
        try {
            return block()
        } finally {
            if (started) {
                val duration = SystemClock.uptimeMillis() - startedAt
                val entry = Mark(label, nowMs() - duration, isMainThread(), Thread.currentThread().name, duration)
                synchronized(lock) { addMarkLocked(entry) }
            }
        }
    }

    /** Accumulates a named counter: calls, total time and time of the first call. */
    fun count(counter: String, durationMs: Long = 0L) {
        if (!started) return
        synchronized(lock) {
            val entry = counters[counter]
            if (entry == null) {
                counters[counter] = longArrayOf(1L, durationMs, nowMs())
            } else {
                entry[0]++
                entry[1] += durationMs
            }
        }
    }

    // ---- Detectors ----------------------------------------------------------------

    /**
     * Samples main-thread responsiveness during the startup window. When the main thread stays
     * stuck longer than [STALL_THRESHOLD_MS], this background thread captures the main thread's
     * current stack (BlockCanary-style), so each stall in the report names the blocking code.
     */
    fun startWatchdog() {
        if (!started) return
        synchronized(lock) {
            if (watchdogStarted) return
            watchdogStarted = true
        }
        val mainHandler = Handler(Looper.getMainLooper())
        val mainThread = Looper.getMainLooper().thread
        val watchdog = Thread({
            val deadline = SystemClock.uptimeMillis() + WATCHDOG_WINDOW_MS
            val pendingSince = AtomicLong(0L)
            var episodeIndex = -1
            while (SystemClock.uptimeMillis() < deadline) {
                val since = pendingSince.get()
                if (since == 0L) {
                    // No pending ping: the main thread answered the previous one, episode over.
                    episodeIndex = -1
                    pendingSince.set(SystemClock.uptimeMillis())
                    mainHandler.post { pendingSince.set(0L) }
                } else {
                    val busyFor = SystemClock.uptimeMillis() - since
                    if (busyFor >= STALL_THRESHOLD_MS) {
                        val stack = try {
                            mainThread.stackTrace
                        } catch (t: Throwable) {
                            emptyArray()
                        }
                        episodeIndex = recordStallSample(episodeIndex, busyFor, stack)
                    }
                }
                try {
                    Thread.sleep(SAMPLE_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "StartupWatchdog")
        watchdog.isDaemon = true
        watchdog.start()
    }

    /** Surfaces disk reads/writes and network calls happening on the main thread. */
    fun installStrictMode() {
        try {
            val builder = StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
            // penaltyListener exists since API 28, but its callback signature changed on
            // API 36 (android.os.strictmode.Violation). This build only compiles against the
            // new signature, so in-app capture is enabled on 36+ and older versions fall back
            // to logcat-only penalties.
            if (Build.VERSION.SDK_INT >= 36) {
                builder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                    recordStrictModeViolation(violation)
                }
            } else {
                builder.penaltyLog()
            }
            StrictMode.setThreadPolicy(builder.build())
        } catch (t: Throwable) {
            Log.w(TAG, "Could not install StrictMode policy", t)
        }
    }

    // ---- Report -------------------------------------------------------------------

    /** Called once the first frame has been drawn; schedules the file report. */
    fun onFirstFrame() {
        if (!started || firstFrameAtMs != null) return
        firstFrameAtMs = nowMs()
        mark("first frame drawn (TTID)")
        synchronized(lock) {
            if (reportScheduled) return
            reportScheduled = true
        }
        val context = appContext ?: return
        reportScope.launch {
            delay(REPORT_DELAY_AFTER_FIRST_FRAME_MS)
            try {
                val text = buildReport(context)
                val file = reportFile(context)
                file.parentFile?.mkdirs()
                file.writeText(text)
                _latestReport.value = text
                Log.i(TAG, "Startup report written to ${file.absolutePath}")
            } catch (t: Throwable) {
                Log.w(TAG, "Could not write startup report", t)
            }
        }
    }

    /** File the report is written to: the app's external files root when available. */
    fun reportFile(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, REPORT_FILE_NAME)
    }

    /** Builds the full text report from everything recorded so far. */
    fun buildReport(context: Context): String {
        val marksCopy: List<Mark>
        val blockersCopy: List<Blocker>
        val countersCopy: Map<String, LongArray>
        val overflow: Boolean
        val strictEvents: Int
        val strictSuppressed: Int
        val firstFrame: Long?
        synchronized(lock) {
            marksCopy = marks.sortedBy { it.atMs }
            blockersCopy = blockers.sortedBy { it.atMs }
            countersCopy = LinkedHashMap(counters)
            overflow = marksOverflow
            strictEvents = strictModeEvents
            strictSuppressed = strictModeSuppressed
            firstFrame = firstFrameAtMs
        }

        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (t: Throwable) {
            "?"
        }
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val usedHeapMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)

        val sb = StringBuilder()
        sb.appendLine("==== OmegaPlayer startup report ====")
        sb.appendLine("App          : $versionName (${if (debuggable) "debuggable" else "release"})")
        sb.appendLine("Device       : ${Build.MANUFACTURER} ${Build.MODEL} - Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("CPU / RAM    : ${Runtime.getRuntime().availableProcessors()} cores, max heap ${maxHeapMb} MB (used at report: ${usedHeapMb} MB)")
        sb.appendLine("Generated at : ${fmtMs(nowMs())} after process start")
        sb.appendLine("Code start   : ${marksCopy.firstOrNull()?.let { fmtMs(it.atMs) } ?: "n/a"} (zygote + class loading happen before this)")
        sb.appendLine("First frame  : ${firstFrame?.let { fmtMs(it) } ?: "not reached yet"} (time to initial display)")
        sb.appendLine()

        sb.appendLine("---- Timeline (time since process start) ----")
        if (marksCopy.isEmpty()) {
            sb.appendLine("(no marks recorded)")
        } else {
            marksCopy.forEach { m ->
                val thread = if (m.mainThread) "main" else m.threadName.take(18)
                val duration = m.durationMs?.let { String.format(Locale.US, "  [block %,dms]", it) } ?: ""
                sb.appendLine(String.format(Locale.US, "%9s | %-18s | %s%s", fmtMs(m.atMs), thread, m.label, duration))
            }
            if (overflow) sb.appendLine("(!) further marks were dropped after $MAX_MARKS entries")
        }
        sb.appendLine()

        sb.appendLine("---- Main-thread phases (time between consecutive main-thread marks) ----")
        val gaps = marksCopy.filter { it.mainThread }
            .zipWithNext { a, b -> Triple(a, b, b.atMs - (a.atMs + (a.durationMs ?: 0L))) }
            .filter { it.third >= GAP_MIN_MS }
            .sortedByDescending { it.third }
            .take(MAX_GAPS)
        if (gaps.isEmpty()) {
            sb.appendLine("(no gap >= ${GAP_MIN_MS}ms)")
        } else {
            gaps.forEach { (a, b, gap) ->
                sb.appendLine(String.format(Locale.US, "%7s  \"%s\" -> \"%s\"", fmtMs(gap), a.label, b.label))
            }
        }
        sb.appendLine()

        sb.appendLine("---- Blocking elements ----")
        val stalls = blockersCopy.filter { it.kind == "main-thread stall" }
        val strict = blockersCopy.filter { it.kind == "StrictMode" }
        val heavyMainBlocks = marksCopy.filter { it.mainThread && (it.durationMs ?: 0L) >= STALL_THRESHOLD_MS }
        if (stalls.isEmpty() && strict.isEmpty() && heavyMainBlocks.isEmpty()) {
            sb.appendLine("(none detected)")
        }
        if (Build.VERSION.SDK_INT < 36) {
            sb.appendLine("(note: StrictMode in-app capture requires Android 16+; the watchdog and measured blocks stay active)")
        }
        stalls.forEach { b ->
            sb.appendLine(String.format(Locale.US, "%9s | main thread was busy for ~%,dms", fmtMs(b.atMs), b.busyMs))
            sb.appendLine("          near: ${b.stackTop}")
            sb.appendLine("          ${b.detail}")
        }
        heavyMainBlocks.forEach { m ->
            sb.appendLine(String.format(Locale.US, "%9s | measured block on main thread: \"%s\" took %,dms", fmtMs(m.atMs), m.label, m.durationMs ?: 0L))
        }
        strict.forEach { b ->
            sb.appendLine(String.format(Locale.US, "%9s | StrictMode %s", fmtMs(b.atMs), b.headline))
            sb.appendLine("          ${b.detail}")
        }
        if (strictSuppressed > 0) {
            sb.appendLine("(!) $strictSuppressed further StrictMode violations were suppressed (cap: $MAX_STRICTMODE_EVENTS, recorded: $strictEvents)")
        }
        sb.appendLine()

        sb.appendLine("---- Counters ----")
        if (countersCopy.isEmpty()) {
            sb.appendLine("(none)")
        } else {
            countersCopy.forEach { (name, v) ->
                val calls = v[0]
                val total = v[1]
                val firstAt = v[2]
                val avg = if (calls > 0) total / calls else 0L
                sb.appendLine(
                    String.format(
                        Locale.US,
                        "%-52s %,6d calls | total %,7dms | avg %,5dms | first %s",
                        name, calls, total, avg, fmtMs(firstAt)
                    )
                )
            }
        }
        sb.appendLine()

        sb.appendLine("---- Data readiness ----")
        listOf(
            "videos flow first emission",
            "audios flow first emission",
            "folder tree rebuilt"
        ).forEach { prefix ->
            val m = marksCopy.firstOrNull { it.label.startsWith(prefix) }
            val value = m?.let {
                val duration = it.durationMs?.let { d -> " (built in ${d}ms)" } ?: ""
                "${fmtMs(it.atMs)}$duration"
            } ?: "not recorded"
            sb.appendLine(String.format(Locale.US, "%-30s %s", prefix, value))
        }
        sb.appendLine()

        sb.appendLine("Notes: the file report is captured ~${REPORT_DELAY_AFTER_FIRST_FRAME_MS / 1000}s after the first")
        sb.appendLine("frame; background work that starts later (thumbnails, syncs) may not appear yet.")
        sb.appendLine("Open this report again from Settings after using the app to include newer events.")

        return sb.toString()
    }

    // ---- Internals -----------------------------------------------------------------

    private fun addMarkLocked(mark: Mark) {
        if (marks.size >= MAX_MARKS) {
            marksOverflow = true
            return
        }
        marks.add(mark)
    }

    /**
     * Creates or updates the blocker entry of the current stall episode. [previousIndex] is the
     * entry created earlier in the same episode (-1 when a new episode starts): its busy time
     * grows as the episode lasts, while the stack captured at onset pinpoints the blocking code.
     */
    private fun recordStallSample(previousIndex: Int, busyMs: Long, stack: Array<StackTraceElement>): Int {
        synchronized(lock) {
            if (previousIndex in blockers.indices) {
                val existing = blockers[previousIndex]
                blockers[previousIndex] = existing.copy(busyMs = busyMs)
                return previousIndex
            }
            if (blockers.size >= MAX_BLOCKERS) return -1
            val appFrame = stack.firstOrNull { it.className.startsWith("com.arslandaim.") } ?: stack.firstOrNull()
            val top = appFrame?.let { "${it.className.substringAfterLast('.')}.${it.methodName} (${it.fileName}:${it.lineNumber})" }
                ?: "unknown (no stack available)"
            val frames = if (stack.isEmpty()) {
                "stack unavailable"
            } else {
                stack.take(16).joinToString("\n          ") { "at $it" }
            }
            blockers.add(
                Blocker(
                    atMs = nowMs() - busyMs,
                    kind = "main-thread stall",
                    headline = "main thread was busy",
                    detail = frames,
                    busyMs = busyMs,
                    stackTop = top
                )
            )
            return blockers.size - 1
        }
    }

    private fun recordBlocker(kind: String, headline: String, detail: String) {
        if (!started) return
        synchronized(lock) {
            if (blockers.size >= MAX_BLOCKERS) return
            blockers.add(Blocker(nowMs(), kind, headline, detail))
        }
    }

    // Takes a plain Throwable so the method signature never references the API 36+
    // android.os.strictmode.* types (Violation extends Throwable).
    private fun recordStrictModeViolation(violation: Throwable) {
        synchronized(lock) {
            if (strictModeEvents >= MAX_STRICTMODE_EVENTS) {
                strictModeSuppressed++
                return
            }
            strictModeEvents++
        }
        val kindName = violation::class.java.simpleName
        val origin = violation.stackTrace.firstOrNull { it.className.startsWith("com.arslandaim.") }
        val location = origin?.let { "${it.fileName ?: "?"}:${it.lineNumber} (${it.className.substringAfterLast('.')})" }
            ?: "unknown location"
        val stack = violation.stackTrace.take(12).joinToString("\n          ") { "at $it" }
        recordBlocker(
            kind = "StrictMode",
            headline = "$kindName on main thread at $location",
            detail = stack
        )
        Log.w(TAG, "$kindName on main thread at $location")
    }

    private fun nowMs(): Long {
        val start = processStartUptime
        return if (start == 0L) 0L else SystemClock.uptimeMillis() - start
    }

    private fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    private fun fmtMs(value: Long): String = String.format(Locale.US, "t+%,dms", value)
}
