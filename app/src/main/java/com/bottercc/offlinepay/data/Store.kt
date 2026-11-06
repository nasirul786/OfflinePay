package com.bottercc.offlinepay.data

import android.content.Context
import com.bottercc.offlinepay.ussd.PayMethod
import com.bottercc.offlinepay.ussd.SessionStatus
import com.bottercc.offlinepay.ussd.UssdTask
import org.json.JSONArray
import org.json.JSONObject

data class Payee(val method: PayMethod, val value: String, val ifsc: String?) {
    val display get() = if (method == PayMethod.ACCOUNT) "A/c $value" else value
}

data class Txn(
    val time: Long,
    val kind: String,
    val title: String,
    val amount: String?,
    val status: SessionStatus,
    val message: String,
) {
    companion object {
        fun from(task: UssdTask, status: SessionStatus, message: String, payeeName: String? = null): Txn {
            val now = System.currentTimeMillis()
            return when (task) {
                is UssdTask.SendMoney -> Txn(now, "send", "Paid ${payeeName ?: task.target}", task.amount, status, message)
                is UssdTask.RequestMoney -> Txn(now, "request", "Requested from ${task.target}", task.amount, status, message)
                is UssdTask.CheckBalance -> Txn(now, "balance", "Balance check", null, status, message)
                is UssdTask.LinkBank -> Txn(now, "link", "Link bank: ${task.bankQuery}", null, status, message)
                else -> Txn(now, "info", task.title, null, status, message)
            }
        }
    }
}

/** Small local store on SharedPreferences. Nothing sensitive (PINs, card details) is ever written here. */
class Store(ctx: Context) {
    private val prefs = ctx.applicationContext.getSharedPreferences("offlinepay", Context.MODE_PRIVATE)

    var simId: String?
        get() = prefs.getString("sim_id", null)
        set(v) = prefs.edit().putString("sim_id", v).apply()

    /** "system", "light" or "dark". */
    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(v) = prefs.edit().putString("theme", v).apply()

    /** Finished onboarding and agreed to the Terms of Use and Privacy Policy. */
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).putLong("agreed_time", System.currentTimeMillis()).apply()

    /** Learned from *99# screens: bank name, last digits of the account, UPI PIN length. */
    var bankName: String?
        get() = prefs.getString("bank_name", null)
        set(v) = prefs.edit().putString("bank_name", v).apply()

    var accountLast4: String?
        get() = prefs.getString("account_last4", null)
        set(v) = prefs.edit().putString("account_last4", v).apply()

    var pinLength: Int
        get() = prefs.getInt("pin_length", 0)
        set(v) = prefs.edit().putInt("pin_length", v).apply()

    /** Last statement fetched from the bank, as joined text, and when. */
    var statement: String?
        get() = prefs.getString("statement", null)
        set(v) = prefs.edit().putString("statement", v).putLong("statement_time", System.currentTimeMillis()).apply()

    val statementTime: Long get() = prefs.getLong("statement_time", 0)

    /** Payments sent from this phone, newest first. */
    fun transactions(): List<Txn> = array("txns").mapObjects {
        Txn(
            time = it.getLong("time"),
            kind = it.getString("kind"),
            title = it.getString("title"),
            amount = it.optString("amount").takeIf { a -> a.isNotEmpty() },
            status = runCatching { SessionStatus.valueOf(it.getString("status")) }.getOrDefault(SessionStatus.UNKNOWN),
            message = it.getString("message"),
        )
    }.filter { it.kind == "send" }

    fun addTransaction(t: Txn) {
        val obj = JSONObject()
            .put("time", t.time).put("kind", t.kind).put("title", t.title)
            .put("amount", t.amount.orEmpty()).put("status", t.status.name).put("message", t.message)
        val list = listOf(obj) + array("txns").mapObjects { it }
        prefs.edit().putString("txns", JSONArray(list.take(200)).toString()).apply()
    }

    fun payees(): List<Payee> = array("payees").mapObjects {
        Payee(PayMethod.valueOf(it.getString("method")), it.getString("value"), it.optString("ifsc").takeIf { s -> s.isNotEmpty() })
    }

    fun savePayee(p: Payee) {
        val rest = payees().filterNot { it.method == p.method && it.value.equals(p.value, ignoreCase = true) }
        val list = (listOf(p) + rest).take(20).map {
            JSONObject().put("method", it.method.name).put("value", it.value).put("ifsc", it.ifsc.orEmpty())
        }
        prefs.edit().putString("payees", JSONArray(list).toString()).apply()
    }

    private fun array(key: String) = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrDefault(JSONArray())

    private fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> =
        (0 until length()).mapNotNull { i -> runCatching { f(getJSONObject(i)) }.getOrNull() }
}
