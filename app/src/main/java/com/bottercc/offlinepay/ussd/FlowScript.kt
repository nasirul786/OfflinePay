package com.bottercc.offlinepay.ussd

sealed interface Decision {
    /** Type [value] into the dialog and press Send. [display] is what the session log shows (PIN masked); [step] is shown to the user. */
    data class Reply(val value: String, val display: String, val step: String) : Decision {
        override fun toString() = "Reply($display)"
    }

    /** The session is over; [success] is null when the outcome can't be told from the text. */
    data class Finish(val success: Boolean?, val message: String) : Decision

    /** Leave this screen to the user but keep watching for the next one. */
    data class WaitForUser(val reason: String) : Decision
}

/**
 * Decides how to answer each *99# screen for a task. Menus differ between banks and operators,
 * so everything is matched by keywords instead of fixed option numbers, and anything unknown
 * is handed to the user rather than guessed.
 *
 * Example (Karur Vysa Bank on Airtel), send to UPI ID:
 *   "Select Option: <BANK> 1. Send Money 3. Check Balance ..."     -> 1
 *   "Send Money to: 1. Mobile No. 3. UPI ID 4. Saved Beneficiary ..." -> 3
 *   "Enter UPI ID."                                                 -> x@upi
 *   "Paying <NAME> , Enter Amount in Rs."                           -> amount
 *   "Enter a remark (Enter 1 to skip)"                              -> remark or 1
 *   "You are paying to ... Enter UPI Pin to proceed 1. Change"      -> PIN
 *   "Your payment to <NAME>, for Rs.1 is successful (RefId: ...) 1. Save contact 2. Exit"
 * Result screens can still have a reply box, so outcomes are detected from the text, not the layout.
 */
class FlowScript(private val task: UssdTask) {

    private val answered = mutableMapOf<String, Int>()

    /** Name the bank resolved for the payee ("Paying KARTHIKEYAN S ,"). */
    var payeeName: String? = null
        private set

    fun decide(s: UssdScreen): Decision {
        PAYING.find(s.text)?.let { payeeName = it.groupValues[1].trim().trimEnd(',').trim() }

        if (task != UssdTask.Console && task !is UssdTask.Probe && task != UssdTask.Transactions) {
            outcome(s)?.let { return it }
        }
        if (!s.hasInput) return finish(s)

        if (task !is UssdTask.Probe && s.option("Send Money", "Balance") == null) {
            s.option("English")?.let { return reply("language", it.key, "English", "Choosing language") }
        }

        val decision = when (task) {
            is UssdTask.SendMoney -> sendMoney(s, task)
            is UssdTask.RequestMoney -> requestMoney(s, task)
            is UssdTask.CheckBalance -> s.option("Check Balance", "Balance")?.let { reply("menu", it.key, it.label, "Opening Check Balance") }
                ?: pinPrompt(s, task.pin)
            is UssdTask.LinkBank -> linkBank(s, task)
            UssdTask.Transactions -> transactions(s)
            is UssdTask.Probe -> task.replies.getOrNull(answered.values.sum())?.let { reply("probe${answered.values.sum()}", it, it, "Probing") }
            UssdTask.Console -> null
        }
        return decision ?: Decision.WaitForUser(
            if (task == UssdTask.Console) "Reply in the *99# dialog. Each screen is recorded here."
            else "This step needs you. Continue in the *99# dialog and OfflinePay will pick it up from there."
        )
    }

    private fun sendMoney(s: UssdScreen, t: UssdTask.SendMoney): Decision? {
        s.option("Send Money")?.let { return reply("menu", it.key, it.label, "Opening Send Money") }
        methodOption(s, t.method)?.let { return reply("method", it.key, it.label, "Choosing ${t.method.label}") }
        pinPrompt(s, t.pin)?.let { return it }
        return paymentFields(s, t.method, t.target, t.ifsc, t.amount, t.remark) ?: confirm(s)
    }

    private fun requestMoney(s: UssdScreen, t: UssdTask.RequestMoney): Decision? {
        s.option("Request Money", "Collect")?.let { return reply("menu", it.key, it.label, "Opening Request Money") }
        if (answered["menu"] == null) s.option("Pending Request")?.let { return reply("menu", it.key, it.label, "Opening requests") }
        methodOption(s, t.method)?.let { return reply("method", it.key, it.label, "Choosing ${t.method.label}") }
        if (s.asks("pin")) return null
        return paymentFields(s, t.method, t.target, null, t.amount, t.remark) ?: confirm(s)
    }

    private val statement = StringBuilder()
    private var statementPages = 0

    /**
     * "6. Transactions" opens "1. Transactions 2. Rewards", then the list comes in pages that end with
     * "0 Next" or "* More" (the last one with "00. Back"). Items can be split across pages, so pages are
     * joined into one text and parsed afterwards.
     */
    private fun transactions(s: UssdScreen): Decision? {
        if (statementPages == 0) {
            s.option("Transaction")?.let { return reply("menu", it.key, it.label, "Opening transactions") }
            pinPrompt(s, null)?.let { return it }
        }
        if ((answered["menu"] ?: 0) == 0) return null
        var more: String? = null
        s.text.lines().forEach { line ->
            val next = MORE.find(line)
            when {
                next != null -> more = next.groupValues[1]
                BACK.matches(line) -> Unit
                else -> statement.append(line.trim()).append(' ')
            }
        }
        statementPages++
        if (more != null && statementPages < MAX_PAGES) {
            return Decision.Reply(more!!, more!!, "Loading more transactions")
        }
        return Decision.Finish(true, statement.toString().trim())
    }

    private fun linkBank(s: UssdScreen, t: UssdTask.LinkBank): Decision? {
        // Each step happens once: after the bank is chosen, the account list also mentions "bank"
        // and picking an account is the user's call.
        if ((answered["bank"] ?: 0) > 0) return null
        if ((answered["profile"] ?: 0) == 0) {
            s.option("My Profile", "Profile")?.let { return reply("profile", it.key, it.label, "Opening My Profile") }
        }
        if ((answered["bankmenu"] ?: 0) == 0) {
            s.option("Change Bank", "Add Bank", "Bank Account", "Bank")?.let { return reply("bankmenu", it.key, it.label, "Opening bank settings") }
        }
        if (s.asks("bank name", "ifsc", "first 4", "first four", "bank")) return reply("bank", t.bankQuery, t.bankQuery, "Finding your bank")
        return null
    }

    private fun methodOption(s: UssdScreen, method: PayMethod): MenuOption? = when (method) {
        PayMethod.MOBILE -> s.option("Mobile")
        PayMethod.UPI_ID -> s.option("UPI ID", "VPA", "Payment Address", "Virtual")
        PayMethod.ACCOUNT -> s.option("IFSC", "A/c", "Account")
    }

    private fun paymentFields(
        s: UssdScreen, method: PayMethod, target: String, ifsc: String?, amount: String, remark: String,
    ): Decision? {
        if (s.asks("remark", "note", "comment", "purpose", "description")) {
            val value = remark.ifBlank { s.skipKey() ?: "Payment" }
            return reply("remark", value, value, "Adding note")
        }
        if (s.asks("amount")) return reply("amount", amount, "₹$amount", "Entering amount")
        if (method == PayMethod.ACCOUNT) {
            val asksIfsc = s.asks("ifsc")
            val asksAcc = s.asks("account", "a/c", "acc no")
            if (asksIfsc && asksAcc) {
                val value = if (s.indexOf("ifsc") < s.indexOf("account", "a/c", "acc no")) "$ifsc $target" else "$target $ifsc"
                return reply("ifsc+acc", value, value, "Entering account details")
            }
            if (asksIfsc) return reply("ifsc", ifsc.orEmpty(), ifsc.orEmpty(), "Entering IFSC")
            if (asksAcc) return reply("account", target, target, "Entering account number")
        }
        if (method == PayMethod.UPI_ID && s.asks("upi id", "vpa", "payment address", "virtual", "upi")) return reply("vpa", target, target, "Finding payee")
        if (method == PayMethod.MOBILE && s.asks("mobile", "phone")) return reply("mobile", target, target, "Finding payee")
        return null
    }

    /** Never re-sends a PIN: a second PIN prompt usually means it was wrong, and retrying could lock the account. */
    private fun pinPrompt(s: UssdScreen, pin: String?): Decision? {
        if (!s.asks("pin")) return null
        if (pin.isNullOrEmpty() || (answered["pin"] ?: 0) > 0) {
            return Decision.WaitForUser("Enter your UPI PIN in the *99# dialog and tap Send.")
        }
        answered["pin"] = 1
        return Decision.Reply(pin, "•".repeat(pin.length), "Verifying UPI PIN")
    }

    private fun confirm(s: UssdScreen): Decision? =
        s.option("Confirm", "Yes", "Proceed", "Continue", "Pay")?.let { reply("confirm", it.key, it.label, "Confirming") }

    private fun reply(tag: String, value: String, display: String, step: String): Decision {
        val n = (answered[tag] ?: 0) + 1
        answered[tag] = n
        if (n > 2) return Decision.WaitForUser("The same question came up again. Please check it and continue in the dialog.")
        return Decision.Reply(value, display, step)
    }

    /** A result screen, with or without a reply box. Null when this is still a question. */
    private fun outcome(s: UssdScreen): Decision.Finish? {
        val t = s.text.lowercase()
        return when {
            NETWORK_ERRORS.any { it in t } || FAIL_WORDS.any { it in t } -> Decision.Finish(false, s.prompt.ifEmpty { s.text })
            SUCCESS.containsMatchIn(t) -> Decision.Finish(true, s.prompt.ifEmpty { s.text })
            task is UssdTask.CheckBalance && "balance is" in t -> Decision.Finish(true, s.prompt.ifEmpty { s.text })
            else -> null
        }
    }

    private fun finish(s: UssdScreen): Decision.Finish {
        val t = s.text.lowercase()
        val networkError = NETWORK_ERRORS.any { it in t }
        val success: Boolean? = when (task) {
            UssdTask.Transactions, UssdTask.Console, is UssdTask.Probe -> !networkError
            else -> null
        }
        return Decision.Finish(if (networkError) false else success, s.text)
    }

    private companion object {
        const val MAX_PAGES = 6 // first page + up to 5 "Next"
        val MORE = Regex("""^\s*(0|\*)\s*[.)]?\s*(next|more)\s*$""", RegexOption.IGNORE_CASE)
        val BACK = Regex("""^\s*0{1,2}\s*[.)]?\s*back\s*$""", RegexOption.IGNORE_CASE)
        val PAYING = Regex("""paying\s+(?!to\b)([A-Za-z][A-Za-z .]{1,60}?)\s*,""", RegexOption.IGNORE_CASE)
        val SUCCESS = Regex("""\bsuccess(ful(ly)?)?\b|ref\s?id|ref\s?no|\brrn\b|has been (sent|raised|initiated)""")
        val NETWORK_ERRORS = listOf("connection problem", "invalid mmi", "unknown application", "network busy")
        val FAIL_WORDS = listOf(
            "fail", "declin", "invalid", "incorrect", "insufficient", "exceed", "unable", "error",
            "sorry", "not registered", "rejected", "timed out", "timeout", "not allowed", "blocked",
        )
    }
}
