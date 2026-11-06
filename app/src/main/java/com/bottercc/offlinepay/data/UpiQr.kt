package com.bottercc.offlinepay.data

import com.bottercc.offlinepay.ussd.PayMethod
import java.net.URLDecoder

/** What the pay form starts with: from a saved payee, a scanned QR, or nothing. */
data class PayDraft(
    val method: PayMethod,
    val target: String = "",
    val ifsc: String? = null,
    val amount: String = "",
    val remark: String = "",
    /** Payee name from the QR ("pn"), shown above the form. */
    val name: String? = null,
) {
    companion object {
        fun from(p: Payee) = PayDraft(p.method, p.value, p.ifsc)
    }
}

/**
 * Reads UPI QR codes: "upi://pay?pa=shop@okbank&pn=Shop&am=120.00&tn=Bill&cu=INR".
 * Merchant (Bharat QR) codes sometimes embed the same link, so the link is searched for anywhere in the text.
 */
object UpiQr {
    private val VPA = Regex("""^[\w.\-]{2,256}@[a-zA-Z][a-zA-Z0-9.\-]{1,64}$""")

    fun parse(raw: String): PayDraft? {
        val start = raw.indexOf("upi://pay", ignoreCase = true)
        if (start < 0) return null
        val query = raw.substring(start).substringAfter('?', "").substringBefore('#')
        val params = query.split('&').mapNotNull { part ->
            val k = part.substringBefore('=', "").lowercase()
            if (k.isEmpty()) null else k to runCatching { URLDecoder.decode(part.substringAfter('='), "UTF-8") }.getOrDefault("")
        }.toMap()
        val vpa = params["pa"]?.trim()?.takeIf { VPA.matches(it) } ?: return null
        val amount = params["am"]?.toDoubleOrNull()?.takeIf { it > 0 }?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(it)
        }.orEmpty()
        val note = params["tn"].orEmpty().filter { it.isLetterOrDigit() || it == ' ' }.trim().take(30)
        val name = params["pn"]?.trim()?.takeIf { it.isNotEmpty() }
        return PayDraft(PayMethod.UPI_ID, vpa, amount = amount, remark = note, name = name)
    }
}
