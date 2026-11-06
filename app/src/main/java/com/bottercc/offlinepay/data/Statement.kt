package com.bottercc.offlinepay.data

import java.text.NumberFormat
import java.util.Locale

data class StatementItem(val credit: Boolean, val amount: String, val party: String, val date: String)

/** Parsing helpers for the text *99# returns. */
object Parse {
    private val ITEM = Regex(
        """(?:^|\s)\d{1,2}\.\s*(Sent|Paid|Debited|Received|Credited|Collected)\s+Rs\.?\s*([\d,]+(?:\.\d{1,2})?)\s+(?:to|from|by)\s+(\S+(?:\s\S+)*?)\s+on\s+(.+?\s(?:AM|PM))""",
        RegexOption.IGNORE_CASE,
    )
    private val BALANCE = Regex("""balance\s+is\s+Rs\.?\s*([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val REF = Regex("""ref\s?(?:id|no)\.?\s*[:\-]?\s*(\w+)""", RegexOption.IGNORE_CASE)
    private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun statement(text: String?): List<StatementItem> {
        val flat = text?.replace(Regex("""\s+"""), " ") ?: return emptyList()
        return ITEM.findAll(flat).map { m ->
            val verb = m.groupValues[1].lowercase()
            StatementItem(
                credit = verb == "received" || verb == "credited" || verb == "collected",
                amount = m.groupValues[2],
                party = m.groupValues[3].trim(),
                date = m.groupValues[4].trim(),
            )
        }.toList()
    }

    fun balance(text: String?): String? = text?.let { BALANCE.find(it)?.groupValues?.get(1) }

    fun refId(text: String?): String? = text?.let { REF.find(it)?.groupValues?.get(1) }

    /** "119637.2" -> "₹1,19,637.20" */
    fun rupees(amount: String): String =
        amount.replace(",", "").toDoubleOrNull()?.let { inr.format(it) } ?: "₹$amount"
}
