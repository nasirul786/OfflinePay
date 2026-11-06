package com.bottercc.offlinepay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bottercc.offlinepay.data.Parse
import com.bottercc.offlinepay.data.PayDraft
import com.bottercc.offlinepay.data.Payee
import com.bottercc.offlinepay.data.StatementItem
import com.bottercc.offlinepay.data.Store
import com.bottercc.offlinepay.data.Txn
import com.bottercc.offlinepay.ussd.Dialer
import com.bottercc.offlinepay.ussd.SimOption
import com.bottercc.offlinepay.ussd.UssdAccessibilityService
import com.bottercc.offlinepay.ussd.UssdController
import com.bottercc.offlinepay.ussd.UssdTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Readiness(
    val callPermission: Boolean,
    val phoneState: Boolean,
    val accessibility: Boolean,
    /** The service is bound right now. Turned on but not running means the system killed it. */
    val serviceRunning: Boolean = true,
) {
    val stopped get() = accessibility && !serviceRunning
    val ready get() = callPermission && accessibility && serviceRunning
}

/** What *99# has told us about the user's linked account. */
data class BankInfo(val name: String?, val last4: String?, val pinLength: Int)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    private val _readiness = MutableStateFlow(readinessNow())
    val readiness: StateFlow<Readiness> = _readiness

    private val _transactions = MutableStateFlow(store.transactions())
    val transactions: StateFlow<List<Txn>> = _transactions

    private val _payees = MutableStateFlow(store.payees())
    val payees: StateFlow<List<Payee>> = _payees

    private val _sims = MutableStateFlow(Dialer.sims(app))
    val sims: StateFlow<List<SimOption>> = _sims

    private val _simId = MutableStateFlow(store.simId)
    val simId: StateFlow<String?> = _simId

    private val _theme = MutableStateFlow(store.theme)
    val theme: StateFlow<String> = _theme

    private val _bank = MutableStateFlow(bankNow())
    val bank: StateFlow<BankInfo> = _bank

    private val _statement = MutableStateFlow(Parse.statement(store.statement))
    val statement: StateFlow<List<StatementItem>> = _statement

    val session = UssdController.state

    /** Task waiting for the PIN screen, and what the pay form starts with (saved payee or scanned QR). */
    var pending: UssdTask? = null
    var draft: PayDraft? = null

    init {
        viewModelScope.launch {
            UssdController.state.collect { if (!it.isActive) refresh() }
        }
    }

    fun refresh() {
        val app = getApplication<Application>()
        _readiness.value = readinessNow()
        // Right after the app starts, Android may still be binding the service; look again shortly.
        if (_readiness.value.stopped) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                _readiness.value = readinessNow()
            }
        }
        _transactions.value = store.transactions()
        _payees.value = store.payees()
        _sims.value = Dialer.sims(app)
        _bank.value = bankNow()
        _statement.value = Parse.statement(store.statement)
    }

    fun selectSim(id: String?) {
        store.simId = id
        _simId.value = id
    }

    val onboarded: Boolean get() = store.onboarded

    fun finishOnboarding() {
        store.onboarded = true
    }

    fun setTheme(mode: String) {
        store.theme = mode
        _theme.value = mode
    }

    fun savePayee(p: Payee) {
        store.savePayee(p)
        _payees.value = store.payees()
    }

    /** Starts a session; returns an error to show, or null when dialing began. */
    fun start(task: UssdTask): String? {
        pending = null
        val sim = store.simId ?: _sims.value.singleOrNull()?.id
        return UssdController.start(getApplication(), task, sim)
    }

    fun cancel() = UssdController.cancel()

    fun resetSession() = UssdController.reset()

    /** The task of the last session (PIN already dropped), or null if there is nothing to retry. */
    fun lastTask(): UssdTask? = UssdController.lastTask?.takeIf { it != UssdTask.Console && it !is UssdTask.Probe }

    private fun bankNow() = BankInfo(store.bankName, store.accountLast4, store.pinLength)

    private fun readinessNow(): Readiness {
        val app = getApplication<Application>()
        return Readiness(
            callPermission = Dialer.hasCallPermission(app),
            phoneState = Dialer.hasPhoneStatePermission(app),
            accessibility = UssdAccessibilityService.isEnabled(app),
            serviceRunning = UssdAccessibilityService.isRunning,
        )
    }
}
