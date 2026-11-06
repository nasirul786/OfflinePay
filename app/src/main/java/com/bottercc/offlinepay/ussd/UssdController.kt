package com.bottercc.offlinepay.ussd

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bottercc.offlinepay.data.Store
import com.bottercc.offlinepay.data.Txn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class SessionStatus { IDLE, DIALING, RUNNING, WAITING_USER, SUCCESS, FAILED, UNKNOWN, CANCELLED }

enum class LogSource { NETWORK, APP, INFO }

data class LogEntry(val source: LogSource, val text: String)

data class SessionState(
    val status: SessionStatus = SessionStatus.IDLE,
    val title: String = "",
    val hint: String? = null,
    val result: String? = null,
    val log: List<LogEntry> = emptyList(),
    val payeeName: String? = null,
) {
    val isActive get() = status == SessionStatus.DIALING || status == SessionStatus.RUNNING || status == SessionStatus.WAITING_USER
}

/** Runs one *99# session at a time: dials, feeds screens from the accessibility service to the script, records the outcome. */
object UssdController {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { end(SessionStatus.FAILED, "No reply from the network in time. The USSD session may have expired.") }

    private var task: UssdTask? = null
    private var script: FlowScript? = null
    private var appContext: Context? = null

    val isActive get() = _state.value.isActive
    val lastTask: UssdTask? get() = task

    fun start(ctx: Context, newTask: UssdTask, simId: String?): String? {
        if (isActive) return "A *99# session is already running."
        if (!UssdAccessibilityService.isEnabled(ctx)) return "Turn on the OfflinePay accessibility service first (Settings)."
        if (!UssdAccessibilityService.isRunning) {
            return "OfflinePay's accessibility service has stopped. Open Settings in OfflinePay to fix it."
        }
        appContext = ctx.applicationContext
        task = newTask
        script = FlowScript(newTask)
        UssdAccessibilityService.instance?.prepareForSession()
        _state.value = SessionState(
            status = SessionStatus.DIALING,
            title = newTask.title,
            hint = "Dialing *99#…",
            log = listOf(LogEntry(LogSource.INFO, "Dialing *99#")),
        )
        Dialer.dial(ctx, simId)?.let { err ->
            end(SessionStatus.FAILED, err)
            return err
        }
        arm(30_000)
        return null
    }

    /** Called by the accessibility service for every new USSD screen. */
    fun onScreen(screen: UssdScreen): Decision? {
        val s = script ?: return null
        if (!isActive) return null
        val decision = s.decide(screen)
        learn(screen.text)
        log(LogSource.NETWORK, screen.text)
        when (decision) {
            is Decision.Reply -> {
                log(LogSource.APP, decision.display)
                val name = s.payeeName
                _state.update {
                    it.copy(
                        status = SessionStatus.RUNNING,
                        hint = decision.step,
                        payeeName = name ?: it.payeeName,
                    )
                }
                arm(60_000)
            }
            is Decision.WaitForUser -> {
                _state.update { it.copy(status = SessionStatus.WAITING_USER, hint = decision.reason) }
                arm(180_000)
            }
            is Decision.Finish -> end(
                when (decision.success) {
                    true -> SessionStatus.SUCCESS
                    false -> SessionStatus.FAILED
                    null -> SessionStatus.UNKNOWN
                },
                decision.message,
            )
        }
        return decision
    }

    fun cancel() {
        if (!isActive) return
        UssdAccessibilityService.instance?.dismissDialog()
        end(SessionStatus.CANCELLED, "You cancelled the session.")
    }

    fun reset() {
        if (!isActive) _state.value = SessionState()
    }

    /** Remembers the bank name, account digits and PIN length that *99# mentions, for the PIN screen. */
    private fun learn(text: String) {
        val store = appContext?.let { Store(it) } ?: return
        BANK.find(text)?.let { store.bankName = it.groupValues[1].trim() }
        ACCOUNT.find(text)?.let { store.accountLast4 = it.groupValues[1].takeLast(4) }
        PIN_LENGTH.find(text)?.let { store.pinLength = it.groupValues[1].toInt() }
    }

    private val BANK = Regex("""Select Option:[ \t]*([^\r\n]+)""", RegexOption.IGNORE_CASE)
    private val ACCOUNT = Regex("""account\s*No\.?\s*X*(\d{3,})""", RegexOption.IGNORE_CASE)
    private val PIN_LENGTH = Regex("""Enter\s*([46])\s*digit\s*UPI\s*PIN""", RegexOption.IGNORE_CASE)

    private fun log(source: LogSource, text: String) = _state.update { it.copy(log = it.log + LogEntry(source, text)) }

    private fun arm(ms: Long) {
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, ms)
    }

    private fun end(status: SessionStatus, message: String) {
        handler.removeCallbacks(timeout)
        if (!isActive) return
        _state.update { it.copy(status = status, hint = null, result = message) }
        val t = task
        // Drop the PIN as soon as the session is over.
        task = t?.withPin(null)
        script = null
        val ctx = appContext ?: return
        if (t == UssdTask.Transactions) {
            if (status == SessionStatus.SUCCESS) Store(ctx).statement = message
            return
        }
        // Only payments are kept; balance checks and other lookups aren't stored.
        if (t is UssdTask.SendMoney && status != SessionStatus.CANCELLED) {
            Store(ctx).addTransaction(Txn.from(t, status, message, _state.value.payeeName))
        }
    }
}
