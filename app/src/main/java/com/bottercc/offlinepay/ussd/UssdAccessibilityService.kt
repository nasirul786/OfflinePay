package com.bottercc.offlinepay.ussd

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bottercc.offlinepay.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads the system USSD dialog and answers it. It ignores every event unless a session
 * started from OfflinePay is running, and only looks at telephony packages.
 */
class UssdAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val process = Runnable { safely("process") { processDialog() } }
    private var lastText: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: SessionOverlay? = null

    override fun onServiceConnected() {
        instance = this
        val o = SessionOverlay(this)
        overlay = o
        scope.launch {
            UssdController.state.collect { st ->
                safely("overlay") { o.render(st) }
                safely("listen") { listen(st.isActive) }
            }
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        safely("destroy") { overlay?.remove() }
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent) = safely("event") {
        if (!UssdController.isActive) return@safely
        if (!isTelephony(event.packageName)) return@safely
        // The dialog fires a burst of events while it lays out; act once it settles.
        handler.removeCallbacks(process)
        handler.postDelayed(process, 350)
    }

    /**
     * Outside a session, only listen to OfflinePay's own windows so the service does next to no work
     * in the background (and HyperOS has nothing to flag). During a session, listen to everything:
     * the USSD dialog's package differs between phone makers.
     */
    private var listeningWide: Boolean? = null

    private fun listen(wide: Boolean) {
        if (listeningWide == wide) return
        val info = serviceInfo ?: return
        info.packageNames = if (wide) null else arrayOf(packageName)
        info.eventTypes = if (wide) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        } else {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        serviceInfo = info
        listeningWide = wide
    }

    /** An exception escaping an accessibility callback kills the process and Android marks the service "malfunctioning". */
    private inline fun safely(where: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, "error in $where", t)
        }
    }

    fun resetSession() {
        lastText = null
    }

    fun dismissDialog() = safely("cancel") {
        val root = dialogRoot() ?: return@safely
        val dialog = read(root)
        if (!click(dialog.buttons, DISMISS_WORDS, preferLast = false)) performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /** Called by the app before dialing, so the first USSD screen is never missed. */
    fun prepareForSession() = safely("prepare") {
        lastText = null
        listen(true)
    }

    private fun processDialog() {
        if (!UssdController.isActive) return
        val root = dialogRoot() ?: return
        val dialog = read(root)
        if (dialog.buttons.isEmpty() && dialog.input == null) return
        val text = dialog.texts.joinToString("\n").trim()
        if (text.isEmpty()) return
        if (dialog.input == null && LOADING_WORDS.any { text.contains(it, ignoreCase = true) }) return
        if (text == lastText) return
        lastText = text

        val d = UssdController.onScreen(UssdScreen.parse(text, dialog.input != null))
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "SCREEN input=${dialog.input != null} buttons=${dialog.buttons.map { it.text }} text=[${text.replace("\n", " ⏎ ")}] -> $d")
        }
        when (d) {
            is Decision.Reply -> {
                val input = dialog.input ?: return
                input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                input.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, d.value) },
                )
                handler.postDelayed({ safely("send") { click(dialog.buttons, SEND_WORDS, preferLast = true) } }, 250)
            }
            // Result screens may still offer options ("1. Save contact 2. Exit"); Cancel ends the session.
            is Decision.Finish -> handler.postDelayed({ safely("dismiss") { click(dialog.buttons, DISMISS_WORDS, preferLast = false) } }, 300)
            is Decision.WaitForUser, null -> Unit
        }
    }

    private fun dialogRoot(): AccessibilityNodeInfo? {
        rootInActiveWindow?.takeIf { isTelephony(it.packageName) }?.let { return it }
        return runCatching { windows.mapNotNull { it.root }.firstOrNull { isTelephony(it.packageName) } }.getOrNull()
    }

    private class Dialog(val texts: List<String>, val input: AccessibilityNodeInfo?, val buttons: List<AccessibilityNodeInfo>)

    private fun read(root: AccessibilityNodeInfo): Dialog {
        val texts = mutableListOf<String>()
        val buttons = mutableListOf<AccessibilityNodeInfo>()
        var input: AccessibilityNodeInfo? = null
        fun walk(n: AccessibilityNodeInfo?) {
            n ?: return
            val cls = n.className?.toString().orEmpty()
            val label = n.text?.toString()?.trim().orEmpty()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "node pkg=${n.packageName} cls=$cls id=${n.viewIdResourceName} click=${n.isClickable} edit=${n.isEditable} text=\"$label\"")
            }
            when {
                n.isEditable || cls.contains("EditText") -> if (input == null) input = n
                cls.contains("Button") || (n.isClickable && label.isNotEmpty() && label.length <= 12) -> buttons += n
                n.viewIdResourceName?.let { it.endsWith(":id/title") || it.endsWith(":id/alertTitle") } == true -> Unit // "Carrier info"
                label.isNotEmpty() -> texts += label
            }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return Dialog(texts, input, buttons)
    }

    private fun click(buttons: List<AccessibilityNodeInfo>, words: List<String>, preferLast: Boolean): Boolean {
        fun label(b: AccessibilityNodeInfo) = (b.text ?: b.contentDescription)?.toString()?.lowercase().orEmpty()
        val target = words.firstNotNullOfOrNull { w -> buttons.firstOrNull { label(it).contains(w) } }
            ?: buttons.filterNot { b -> NEGATIVE_WORDS.any { label(b).contains(it) } }
                .let { if (preferLast) it.lastOrNull() else it.firstOrNull() }
            ?: return false
        var node: AccessibilityNodeInfo? = target
        while (node != null && !node.isClickable) node = node.parent
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    companion object {
        @Volatile
        var instance: UssdAccessibilityService? = null
            private set

        private const val TAG = "OfflinePayUSSD"
        private val SEND_WORDS = listOf("send", "reply", "submit", "ok", "yes", "answer")
        private val DISMISS_WORDS = listOf("ok", "dismiss", "close", "done", "cancel")
        private val NEGATIVE_WORDS = listOf("cancel", "dismiss", "close", "no")
        private val LOADING_WORDS = listOf("running", "please wait", "loading", "sending")

        fun isTelephony(pkg: CharSequence?): Boolean {
            val p = pkg?.toString()?.lowercase() ?: return false
            return p == "com.android.phone" || p.endsWith(".phone") || "telephon" in p || "telecom" in p || "ussd" in p
        }

        /** Enabled in Settings and actually bound right now. Enabled-but-not-running is the "malfunctioning" state. */
        val isRunning: Boolean get() = instance != null

        fun isEnabled(ctx: Context): Boolean {
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(ctx, UssdAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
