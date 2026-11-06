package com.bottercc.offlinepay.ui

import androidx.compose.foundation.Image
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.BankInfo
import com.bottercc.offlinepay.R
import com.bottercc.offlinepay.ui.theme.LocalDarkTheme
import com.bottercc.offlinepay.ussd.UssdTask

private class PinColors(
    val page: Color, val bar: Color, val text: Color, val muted: Color, val card: Color,
    val pad: Color, val key: Color, val del: Color, val pay: Color, val payText: Color, val dot: Color,
)

private val LightPin = PinColors(
    page = Color.White, bar = Color.White, text = Color(0xFF1B1B1F), muted = Color(0xFF6E6E78),
    card = Color(0xFFEEF0F4), pad = Color(0xFFF3F5F8), key = Color(0xFFE6EAF0), del = Color(0xFFC8D5EE),
    pay = Color(0xFF1B2B7A), payText = Color.White, dot = Color(0xFF5A5A66),
)

private val DarkPin = PinColors(
    page = Color(0xFF000000), bar = Color(0xFF0E0E0E), text = Color(0xFFEDEDED), muted = Color(0xFFA0A0A8),
    card = Color(0xFF1C1C1F), pad = Color(0xFF0E0E0E), key = Color(0xFF232327), del = Color(0xFF2C3650),
    pay = Color(0xFF4C63D2), payText = Color.White, dot = Color(0xFFB0B0B8),
)


@Composable
fun PinScreen(task: UssdTask, bank: BankInfo, onBack: () -> Unit, onPin: (String) -> Unit) {
    val c = if (LocalDarkTheme.current) DarkPin else LightPin
    var pin by remember { mutableStateOf("") }
    // The bank tells us the PIN length ("Enter 4 digit UPI PIN") the first time; until then allow 4 or 6.
    val length = bank.pinLength.takeIf { it == 4 || it == 6 }
    val dots = length ?: 6
    val canSubmit = if (length != null) pin.length == length else pin.length == 4 || pin.length == 6

    val (headline, sub) = when (task) {
        is UssdTask.SendMoney -> "Pay ₹${task.amount}.00" to "To ${if (task.ifsc != null) "A/c ${task.target}" else task.target}"
        is UssdTask.CheckBalance -> "Check balance" to (bank.last4?.let { "Account XXXX$it" } ?: "Your linked account")
        else -> task.title to ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.page),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.bar)
                .statusBarsPadding(),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text("OfflinePay", color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = c.text) }
            }
            Text(
                bank.name?.let { prettyBank(it) } ?: "Offline UPI payment",
                color = c.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 10.dp),
            )
        }

        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(c.card, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(headline, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                if (sub.isNotEmpty()) Text(sub, color = c.muted, fontSize = 13.sp, maxLines = 1)
            }
            Text("₹→", color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .background(c.pay, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Person, contentDescription = null, tint = c.payText, modifier = Modifier.size(18.dp)) }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Enter your PIN",
            color = c.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(22.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(dots) { i ->
                val filled = i < pin.length
                val fill by animateColorAsState(if (filled) c.dot else Color.Transparent, tween(120), label = "pinDot")
                val pop by animateFloatAsState(if (filled) 1f else 0.85f, spring(dampingRatio = 0.4f, stiffness = 700f), label = "pinPop")
                Box(
                    Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .clip(CircleShape)
                        .background(fill)
                        .border(1.5.dp, c.dot, CircleShape),
                )
            }
        }
        Spacer(Modifier.weight(1.3f))

        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFFE39B00), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Never enter your UPI PIN to receive money", color = c.muted, fontSize = 12.sp)
        }

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "del", "0", "pay")
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.pad)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            keys.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        val bg = when (k) {
                            "del" -> c.del
                            "pay" -> if (canSubmit) c.pay else c.pay.copy(alpha = 0.45f)
                            else -> c.key
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .pressable(enabled = k != "pay" || canSubmit, scale = 0.93f, dim = 0.75f) {
                                    when (k) {
                                        "del" -> pin = pin.dropLast(1)
                                        "pay" -> onPin(pin)
                                        else -> if (pin.length < dots) pin += k
                                    }
                                }
                                .clip(RoundedCornerShape(25.dp))
                                .background(bg),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (k) {
                                "del" -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete", tint = c.text)
                                "pay" -> Text(
                                    if (task is UssdTask.SendMoney) "Pay" else "Submit",
                                    color = c.payText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                )
                                else -> Text(k, color = c.text, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
