package com.bottercc.offlinepay.ussd

enum class PayMethod(val label: String) {
    MOBILE("Mobile number"),
    UPI_ID("UPI ID"),
    ACCOUNT("Bank account"),
}

/** One thing the user wants to do over *99#. The UPI PIN, when present, only lives in memory for one session. */
sealed class UssdTask(val title: String) {

    data class SendMoney(
        val method: PayMethod,
        val target: String,
        val ifsc: String?,
        val amount: String,
        val remark: String,
        val pin: String? = null,
    ) : UssdTask("Send money") {
        override fun toString() = "SendMoney($method, $target, ₹$amount)"
    }

    data class RequestMoney(
        val method: PayMethod,
        val target: String,
        val amount: String,
        val remark: String,
    ) : UssdTask("Request money")

    data class CheckBalance(val pin: String? = null) : UssdTask("Check balance") {
        override fun toString() = "CheckBalance"
    }

    data class LinkBank(val bankQuery: String) : UssdTask("Link bank account")

    data object Transactions : UssdTask("Recent transactions")

    data object Console : UssdTask("*99# console")

    /** Debug only: sends the given replies in order, then stops. Never used for PINs. */
    data class Probe(val replies: List<String>) : UssdTask("Probe")

    val needsPin: Boolean get() = this is SendMoney || this is CheckBalance

    fun withPin(pin: String?): UssdTask = when (this) {
        is SendMoney -> copy(pin = pin)
        is CheckBalance -> copy(pin = pin)
        else -> this
    }

    fun pinOrNull(): String? = when (this) {
        is SendMoney -> pin
        is CheckBalance -> pin
        else -> null
    }
}
