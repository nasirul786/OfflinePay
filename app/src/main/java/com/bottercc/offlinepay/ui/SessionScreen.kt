package com.bottercc.offlinepay.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.BankInfo
import com.bottercc.offlinepay.MainViewModel
import com.bottercc.offlinepay.data.Parse
import com.bottercc.offlinepay.ui.theme.LocalIos
import com.bottercc.offlinepay.ussd.LogEntry
import com.bottercc.offlinepay.ussd.LogSource
import com.bottercc.offlinepay.ussd.SessionState
import com.bottercc.offlinepay.ussd.SessionStatus
import com.bottercc.offlinepay.ussd.UssdTask

/** "KARUR VYSA BANK" -> "Karur Vysa Bank" */
fun prettyBank(name: String) = name.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

@Composable
fun SessionScreen(vm: MainViewModel, onDone: () -> Unit, onRetry: () -> Unit) {
    val ios = LocalIos.current
    val s by vm.session.collectAsState()
    val bank by vm.bank.collectAsState()
    val task = vm.lastTask()
    var showLog by remember { mutableStateOf(false) }
    val finished = !s.isActive && s.status != SessionStatus.IDLE
    val page = if (finished && task != UssdTask.Transactions) ios.card else ios.groupedBg

    Column(
        Modifier
            .fillMaxSize()
            .background(page)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val stage = when {
                !finished -> 0
                task == UssdTask.Transactions && s.status == SessionStatus.SUCCESS -> 2
                else -> 1
            }
            // Progress fades and settles into the result.
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    (fadeIn(tween(350, delayMillis = 80)) + scaleIn(tween(350, delayMillis = 80), initialScale = 0.94f))
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "sessionStage",
            ) { st ->
                when (st) {
                    0 -> Progress(s)
                    2 -> Statement(s)
                    else -> Result(s, task, bank)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                if (showLog) "Hide session details" else "Show session details",
                color = ios.accent,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { showLog = !showLog }
                    .padding(8.dp),
            )
            AnimatedVisibility(showLog, enter = expandVertically(tween(300)) + fadeIn(tween(300)), exit = shrinkVertically(tween(250)) + fadeOut(tween(200))) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.log.forEach { LogBubble(it) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Bottom action bar, like the system "DONE" bar.
        HorizontalDivider(thickness = 0.5.dp, color = ios.separator)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(52.dp)) {
            if (s.isActive) {
                BarButton("Cancel", ios.red, Modifier.weight(1f), vm::cancel)
            } else {
                if (s.status != SessionStatus.SUCCESS && task != null) {
                    BarButton("Try again", ios.accent, Modifier.weight(1f), onRetry)
                }
                BarButton("Done", ios.accent, Modifier.weight(1f), onDone)
            }
        }
    }
}

@Composable
private fun BarButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxSize().pressable(scale = 1f, dim = 0.45f, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text.uppercase(), color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun Progress(s: SessionState) {
    val ios = LocalIos.current
    Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (s.status == SessionStatus.WAITING_USER) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = ios.orange, modifier = Modifier.size(64.dp))
        } else {
            CircularProgressIndicator(Modifier.size(56.dp), color = ios.accent, strokeWidth = 4.dp)
        }
        Spacer(Modifier.height(24.dp))
        Text(if (s.status == SessionStatus.WAITING_USER) "Your turn" else s.title, color = ios.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        s.hint?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = ios.secondaryLabel, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Result(s: SessionState, task: UssdTask?, bank: BankInfo) {
    val ios = LocalIos.current
    val ok = s.status == SessionStatus.SUCCESS
    val failed = s.status == SessionStatus.FAILED
    val balance = Parse.balance(s.result)
    val bankLine = listOfNotNull(bank.name?.let { prettyBank(it) }, bank.last4).joinToString(" - ").ifEmpty { null }

    Column(Modifier.fillMaxWidth().padding(top = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ResultMark(
            when {
                ok -> ios.green
                failed -> ios.red
                else -> ios.orange
            },
            success = ok,
        )
        Spacer(Modifier.height(28.dp))
        val title = when {
            ok && task is UssdTask.CheckBalance -> "Bank balance fetched\nsuccessfully"
            ok && task is UssdTask.SendMoney -> "Payment successful"
            ok && task is UssdTask.RequestMoney -> "Request sent"
            ok -> "Done"
            failed && task is UssdTask.CheckBalance -> "Couldn't fetch balance"
            failed && task is UssdTask.SendMoney -> "Payment failed"
            failed -> "Didn't go through"
            s.status == SessionStatus.CANCELLED -> "Cancelled"
            else -> "Session finished"
        }
        Text(title, color = ios.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 26.sp)

        if (task is UssdTask.CheckBalance && bankLine != null) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).background(ios.blue, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(bankLine, color = ios.label, fontSize = 17.sp)
            }
        }

        when {
            ok && task is UssdTask.CheckBalance && balance != null -> {
                Spacer(Modifier.height(10.dp))
                Text(Parse.rupees(balance).replace("₹", "₹ "), color = ios.label, fontSize = 34.sp, fontWeight = FontWeight.Medium)
            }
            ok && task is UssdTask.SendMoney -> {
                Spacer(Modifier.height(20.dp))
                Text(Parse.rupees(task.amount).replace("₹", "₹ "), color = ios.label, fontSize = 34.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Paid to ${s.payeeName ?: task.target}", color = ios.label, fontSize = 17.sp)
                if (s.payeeName != null) Text(task.target, color = ios.secondaryLabel, fontSize = 15.sp)
                Parse.refId(s.result)?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "UPI Ref ID: $it",
                        color = ios.secondaryLabel,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(ios.fill, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            else -> s.result?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = ios.secondaryLabel, fontSize = 15.sp, textAlign = TextAlign.Center)
                if (s.status == SessionStatus.UNKNOWN) {
                    Spacer(Modifier.height(6.dp))
                    Text("Check the SMS from your bank to confirm.", color = ios.secondaryLabel, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Outlined circle with a check (or cross) that draws itself in. */
@Composable
private fun ResultMark(color: Color, success: Boolean) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(650)) }
    Canvas(Modifier.size(112.dp)) {
        val stroke = 6.dp.toPx()
        drawCircle(color, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        val w = size.width
        val path = Path().apply {
            if (success) {
                moveTo(w * 0.29f, w * 0.52f)
                lineTo(w * 0.44f, w * 0.66f)
                lineTo(w * 0.72f, w * 0.38f)
            } else {
                moveTo(w * 0.36f, w * 0.36f)
                lineTo(w * 0.64f, w * 0.64f)
                moveTo(w * 0.64f, w * 0.36f)
                lineTo(w * 0.36f, w * 0.64f)
            }
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.value, partial, true)
        drawPath(partial, color, style = Stroke(stroke, cap = StrokeCap.Round))
        if (!success && progress.value >= 1f) drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun Statement(s: SessionState) {
    val ios = LocalIos.current
    val items = Parse.statement(s.result)
    Column(Modifier.fillMaxWidth()) {
        Text("Recent transactions", color = ios.label, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 16.dp))
        if (items.isEmpty()) {
            SectionCard { Text(s.result.orEmpty(), color = ios.label, fontSize = 15.sp) }
        } else {
            SectionCard(padded = false, footer = "Latest ${items.size} transactions from your bank.") {
                items.forEachIndexed { i, it ->
                    if (i > 0) IosDivider(68.dp)
                    StatementRow(it)
                }
            }
        }
    }
}

@Composable
private fun LogBubble(e: LogEntry) {
    val ios = LocalIos.current
    val mine = e.source == LogSource.APP
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        if (e.source == LogSource.INFO) {
            Text(e.text, color = ios.secondaryLabel, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else {
            // iMessage-style bubbles: the app's replies in the accent colour, the bank's in grey.
            Text(
                e.text,
                color = if (mine) Color.White else ios.label,
                fontSize = 14.sp,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .background(if (mine) ios.accent else ios.fill, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
