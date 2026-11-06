package com.bottercc.offlinepay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.ui.theme.LocalIos

/** Explains the Accessibility Service in a few lines and sends the user to switch it on. */
@Composable
fun AccessibilityScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val ios = LocalIos.current
    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg),
    ) {
        SubTopBar("Accessibility", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            IconSquircle(Icons.Filled.Accessibility, ios.indigo, 56.dp, 14.dp)
            Spacer(Modifier.height(14.dp))
            Text("Turn On Accessibility", color = ios.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "OfflinePay needs it to fill in the *99# steps for you.",
                color = ios.secondaryLabel,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            SectionCard(padded = false) {
                Point(Icons.Filled.Visibility, ios.blue, "Reads only the *99# dialog", "And only while your payment is running")
                IosDivider(58.dp)
                Point(Icons.Filled.TouchApp, ios.green, "Fills in each step", "Payee, amount, note and UPI PIN")
                IosDivider(58.dp)
                Point(Icons.Filled.Block, ios.red, "Ignores other apps", "Nothing is shared and your PIN is never saved")
            }
            Spacer(Modifier.height(20.dp))

            SectionCard("How to turn it on") {
                InfoSteps(listOf("Tap Open Settings below", "Open Downloaded apps → OfflinePay", "Turn on Use OfflinePay"))
            }
            Spacer(Modifier.height(16.dp))
        }
        IosButton(
            "Open Settings",
            onClick = onOpenSettings,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun Point(icon: ImageVector, color: Color, title: String, subtitle: String) {
    val ios = LocalIos.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        IconSquircle(icon, color, 30.dp, 7.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ios.label, fontSize = 16.sp)
            Text(subtitle, color = ios.secondaryLabel, fontSize = 13.sp)
        }
    }
}
