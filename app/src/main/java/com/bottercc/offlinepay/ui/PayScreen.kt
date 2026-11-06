package com.bottercc.offlinepay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.data.PayDraft
import com.bottercc.offlinepay.data.Payee
import com.bottercc.offlinepay.ui.theme.LocalIos
import com.bottercc.offlinepay.ussd.PayMethod
import com.bottercc.offlinepay.ussd.UssdTask

const val USSD_LIMIT = 5000

private val IFSC = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
private val VPA = Regex("^[\\w.\\-]{2,256}@[a-zA-Z][a-zA-Z0-9.\\-]{1,64}$")
private val AMOUNT = Regex("^\\d{0,4}(\\.\\d{0,2})?$")

@Composable
fun PayScreen(
    request: Boolean,
    initialMethod: PayMethod,
    draft: PayDraft?,
    onBack: () -> Unit,
    onSubmit: (UssdTask, Payee) -> Unit,
) {
    val ios = LocalIos.current
    var method by remember { mutableStateOf(draft?.method ?: initialMethod) }
    var target by remember { mutableStateOf(draft?.target.orEmpty()) }
    var ifsc by remember { mutableStateOf(draft?.ifsc.orEmpty()) }
    var amount by remember { mutableStateOf(draft?.amount.orEmpty()) }
    var remark by remember { mutableStateOf(draft?.remark.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }
    val fromQr = draft?.name != null

    val methods = if (request) listOf(PayMethod.MOBILE, PayMethod.UPI_ID) else PayMethod.entries
    val targetError = when (method) {
        PayMethod.MOBILE -> if (target.length != 10 || target.first() !in '6'..'9') "Enter a 10-digit mobile number" else null
        PayMethod.UPI_ID -> if (!VPA.matches(target)) "Enter a valid UPI ID, like name@bank" else null
        PayMethod.ACCOUNT -> if (target.length !in 6..18) "Enter the account number" else null
    }
    val ifscError = if (method == PayMethod.ACCOUNT && !IFSC.matches(ifsc)) "Enter an 11-character IFSC, like SBIN0001234" else null
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val amountError = when {
        amountValue <= 0 -> "Enter an amount"
        amountValue > USSD_LIMIT -> "Offline payments are limited to ₹5,000"
        else -> null
    }
    val valid = targetError == null && ifscError == null && amountError == null
    val amountLabel = if (amountValue > 0) com.bottercc.offlinepay.data.Parse.rupees(amount) else ""

    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg),
    ) {
        SubTopBar(if (request) "Request Money" else "Send Money", onBack, backLabel = "Cancel")
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (fromQr) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(draft?.name.orEmpty(), 64)
                    Spacer(Modifier.height(10.dp))
                    Text(draft?.name.orEmpty(), color = ios.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(target, color = ios.secondaryLabel, fontSize = 15.sp)
                }
            } else {
                IosSegmented(
                    options = methods.map { if (it == PayMethod.ACCOUNT) "Bank A/c" else if (it == PayMethod.MOBILE) "Mobile" else it.label },
                    selected = methods.indexOf(method).coerceAtLeast(0),
                    onSelect = { i ->
                        if (methods[i] != method) {
                            method = methods[i]
                            target = ""
                            showErrors = false
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
                SectionCard(if (request) "Request from" else "Pay to") {
                    when (method) {
                        PayMethod.MOBILE -> IosField(
                            value = target,
                            onValueChange = { v -> target = v.filter { it.isDigit() }.takeLast(10) },
                            placeholder = "Mobile number",
                            prefix = "+91",
                            error = targetError.takeIf { showErrors },
                            hint = "Pays the UPI account linked to this number",
                            keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )
                        PayMethod.UPI_ID -> IosField(
                            value = target,
                            onValueChange = { target = it.trim() },
                            placeholder = "name@bank",
                            error = targetError.takeIf { showErrors },
                            keyboard = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        PayMethod.ACCOUNT -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosField(
                                value = target,
                                onValueChange = { v -> target = v.filter { it.isDigit() }.take(18) },
                                placeholder = "Account number",
                                error = targetError.takeIf { showErrors },
                                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            IosField(
                                value = ifsc,
                                onValueChange = { v -> ifsc = v.uppercase().filter { it.isLetterOrDigit() }.take(11) },
                                placeholder = "IFSC",
                                error = ifscError.takeIf { showErrors },
                                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            )
                        }
                    }
                }
            }

            // Big centred amount, like Apple Cash.
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹", color = if (amount.isEmpty()) ios.secondaryLabel else ios.label, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    BasicTextField(
                        value = amount,
                        onValueChange = { v -> if (AMOUNT.matches(v)) amount = v.trimStart('0').let { if (it.startsWith(".")) "0$it" else it } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = ios.label),
                        cursorBrush = SolidColor(ios.accent),
                        modifier = Modifier.widthIn(min = 40.dp, max = 240.dp),
                        decorationBox = { inner ->
                            if (amount.isEmpty()) Text("0", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = ios.secondaryLabel.copy(alpha = 0.5f))
                            inner()
                        },
                    )
                }
                Text(
                    if (showErrors && amountError != null) amountError else "Maximum ₹5,000 per offline payment",
                    color = if (showErrors && amountError != null) ios.red else ios.secondaryLabel,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("100", "500", "1000", "2000").forEach { v -> IosChip("₹$v", amount == v) { amount = v } }
                }
            }

            IosField(
                value = remark,
                onValueChange = { v -> remark = v.filter { it.isLetterOrDigit() || it == ' ' }.take(30) },
                placeholder = "Add a note (optional)",
            )

            Footnote(
                if (request) "OfflinePay dials *99# and sends a collect request. They approve it in their UPI app."
                else "OfflinePay dials *99# and fills in every step for you. Keep the phone on this screen until it finishes.",
                Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
        }

        IosButton(
            text = (if (request) "Request " else "Pay ") + amountLabel,
            onClick = {
                showErrors = true
                if (!valid) return@IosButton
                val payee = Payee(method, target, ifsc.takeIf { method == PayMethod.ACCOUNT })
                val task = if (request) UssdTask.RequestMoney(method, target, amount, remark.trim())
                else UssdTask.SendMoney(method, target, payee.ifsc, amount, remark.trim())
                onSubmit(task, payee)
            },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

/** Numbered steps, used by the bank-linking screen. */
@Composable
fun InfoSteps(steps: List<String>) {
    val ios = LocalIos.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${i + 1}",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(ios.accent, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(s, color = ios.label, fontSize = 15.sp)
            }
        }
    }
}
