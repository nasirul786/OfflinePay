package com.bottercc.offlinepay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.MainViewModel
import com.bottercc.offlinepay.R
import com.bottercc.offlinepay.data.Payee
import com.bottercc.offlinepay.ui.theme.Brand
import com.bottercc.offlinepay.ui.theme.BrandDeep
import com.bottercc.offlinepay.ui.theme.LocalIos
import com.bottercc.offlinepay.ussd.PayMethod

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onScan: () -> Unit,
    onPay: (request: Boolean, method: PayMethod) -> Unit,
    onPayee: (Payee) -> Unit,
    onBalance: () -> Unit,
    onLinkBank: () -> Unit,
    onStatement: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val ios = LocalIos.current
    val readiness by vm.readiness.collectAsState()
    val payees by vm.payees.collectAsState()
    val txns by vm.transactions.collectAsState()
    var opened by remember { mutableStateOf<com.bottercc.offlinepay.data.Txn?>(null) }
    opened?.let { PaymentDialog(it) { opened = null } }
    val sims by vm.sims.collectAsState()
    val simId by vm.simId.collectAsState()
    val simLabel = sims.firstOrNull { it.id == simId }?.label ?: sims.singleOrNull()?.label ?: "Default SIM"

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { Header(simLabel, onSettings) }

        if (!readiness.ready) {
            item {
                SectionCard(padded = false) {
                    IosRow(
                        title = if (readiness.stopped) "Accessibility service stopped" else "Finish setup",
                        subtitle = if (readiness.stopped) "Your phone closed it in the background. Tap to fix."
                        else "Allow calls and turn on the accessibility service",
                        icon = Icons.Filled.WarningAmber,
                        iconColor = ios.orange,
                        chevron = true,
                        onClick = onSettings,
                    )
                }
            }
        }

        item {
            SectionCard("Payments & account") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        ActionTile(Icons.Filled.PhoneIphone, "To mobile", ios.green, Modifier.weight(1f)) { onPay(false, PayMethod.MOBILE) }
                        ActionTile(Icons.Filled.AlternateEmail, "To UPI ID", ios.blue, Modifier.weight(1f)) { onPay(false, PayMethod.UPI_ID) }
                        ActionTile(Icons.Filled.QrCodeScanner, "Scan QR", ios.accent, Modifier.weight(1f), onScan)
                        ActionTile(Icons.Filled.AccountBalance, "To bank a/c", ios.indigo, Modifier.weight(1f)) { onPay(false, PayMethod.ACCOUNT) }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        ActionTile(Icons.AutoMirrored.Filled.CallReceived, "Request", ios.orange, Modifier.weight(1f)) { onPay(true, PayMethod.MOBILE) }
                        ActionTile(Icons.Filled.AccountBalanceWallet, "Balance", ios.teal, Modifier.weight(1f), onBalance)
                        ActionTile(Icons.Filled.AddCard, "Link bank", ios.purple, Modifier.weight(1f), onLinkBank)
                        ActionTile(Icons.AutoMirrored.Filled.ReceiptLong, "Statement", ios.gray, Modifier.weight(1f), onStatement)
                    }
                }
            }
        }

        if (payees.isNotEmpty()) {
            item {
                SectionCard("Recent payees") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(payees) { p ->
                            Column(
                                Modifier
                                    .width(64.dp)
                                    .pressable(scale = 0.9f) { onPayee(p) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Avatar(p.value, 52)
                                Spacer(Modifier.height(6.dp))
                                Text(p.display, color = ios.label, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                HeaderWithAction("Recent payments", if (txns.isNotEmpty()) "See All" else null, onHistory)
                SectionCard(padded = txns.isEmpty()) {
                    if (txns.isEmpty()) {
                        Footnote("Payments you send with OfflinePay show up here.")
                    } else {
                        txns.take(5).forEachIndexed { i, t ->
                            if (i > 0) IosDivider(68.dp)
                            TxnRow(t) { opened = t }
                        }
                    }
                }
            }
        }

        item {
            Footnote(
                "Uses NPCI's *99# USSD service · up to ₹5,000 per offline payment · your operator may charge up to ₹0.50",
                Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

/** Curved orange header that runs edge to edge, past the list's side padding. */
@Composable
private fun Header(simLabel: String, onSettings: () -> Unit) {
    Column(
        Modifier
            .layout { measurable, c ->
                val extra = 32.dp.roundToPx()
                val w = c.maxWidth + extra
                val p = measurable.measure(c.copy(minWidth = w, maxWidth = w))
                layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
            }
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.verticalGradient(listOf(Brand, BrandDeep)))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 26.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("OfflinePay", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("UPI payments without internet", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
            HeaderIcon(Icons.Filled.Settings, "Settings", onSettings)
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable(onClick = onSettings)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.SignalCellularAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Paying from $simLabel", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HeaderWithAction(title: String, action: String?, onAction: () -> Unit) {
    val ios = LocalIos.current
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(title.uppercase(), color = ios.secondaryLabel, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = ios.accent,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
