package com.bottercc.offlinepay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.MainViewModel
import com.bottercc.offlinepay.ui.theme.LocalIos

@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ios = LocalIos.current
    val txns by vm.transactions.collectAsState()
    var opened by remember { mutableStateOf<com.bottercc.offlinepay.data.Txn?>(null) }
    opened?.let { PaymentDialog(it) { opened = null } }
    Column(Modifier.fillMaxSize().background(ios.groupedBg)) {
        SubTopBar("Payments", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            if (txns.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No Payments", color = ios.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Footnote("Payments you send with OfflinePay show up here.")
                }
            } else {
                Spacer(Modifier.height(12.dp))
                SectionCard(padded = false, footer = "Tap a payment to see its details. Stored on this phone only.") {
                    txns.forEachIndexed { i, t ->
                        if (i > 0) IosDivider(68.dp)
                        TxnRow(t) { opened = t }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LinkBankScreen(onBack: () -> Unit, onStart: (String) -> Unit) {
    val ios = LocalIos.current
    var bank by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(ios.groupedBg)) {
        SubTopBar("Link Bank", onBack)
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(0.dp))
            SectionCard("Your bank", footer = "The SIM must be the number registered with your bank.") {
                IosField(
                    value = bank,
                    onValueChange = { bank = it.take(20) },
                    placeholder = "Bank name or first 4 letters of IFSC",
                )
            }
            SectionCard("What happens", footer = "OfflinePay never sees or stores your card details.") {
                InfoSteps(
                    listOf(
                        "OfflinePay dials *99# from your SIM and enters your bank.",
                        "Your bank lists the accounts linked to this number. Pick yours in the dialog.",
                        "Type the last 6 digits of your debit card and its expiry, then set or confirm your UPI PIN.",
                    ),
                )
            }
        }
        IosButton(
            "Start Linking",
            onClick = { onStart(bank.trim()) },
            enabled = bank.trim().length >= 3,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}
