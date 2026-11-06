package com.bottercc.offlinepay.ui

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.BuildConfig
import com.bottercc.offlinepay.MainViewModel
import com.bottercc.offlinepay.ui.theme.LocalIos

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onAccessibility: () -> Unit) {
    val ios = LocalIos.current
    val ctx = LocalContext.current
    val readiness by vm.readiness.collectAsState()
    val sims by vm.sims.collectAsState()
    val simId by vm.simId.collectAsState()
    val theme by vm.theme.collectAsState()
    var repair by remember { mutableStateOf(false) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refresh() }

    /** Green tick when granted, otherwise an "Allow" (or "Fix") action. */
    @Composable
    fun Status(done: Boolean, action: String = "Allow", color: Color = ios.accent, onFix: () -> Unit) {
        if (done) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Allowed", tint = ios.green, modifier = Modifier.size(22.dp))
        } else {
            Text(
                action,
                color = color,
                fontSize = 16.sp,
                modifier = Modifier
                    .pressable(scale = 1f, dim = 0.4f, onClick = onFix)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg),
    ) {
        SubTopBar("Settings", onBack, backLabel = "Home")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(0.dp))

            SectionCard("Permissions", padded = false) {
                IosRow("Phone calls", icon = Icons.Filled.Phone, iconColor = ios.green) {
                    Status(readiness.callPermission) {
                        permissions.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
                    }
                }
                IosDivider(60.dp)
                IosRow("SIM access", icon = Icons.Filled.SimCard, iconColor = ios.blue) {
                    Status(readiness.phoneState) { permissions.launch(arrayOf(Manifest.permission.READ_PHONE_STATE)) }
                }
                IosDivider(60.dp)
                IosRow(
                    "Accessibility",
                    subtitle = if (readiness.stopped) "Stopped by the system" else null,
                    icon = Icons.Filled.Accessibility,
                    iconColor = ios.indigo,
                ) {
                    if (readiness.stopped) Status(false, "Fix", ios.orange) { repair = true }
                    else Status(readiness.accessibility, onFix = onAccessibility)
                }
            }

            SectionCard("SIM", padded = false, footer = "Use the SIM registered with your bank.") {
                when {
                    sims.size > 1 -> {
                        val options = listOf<Pair<String?, String>>(null to "System default") + sims.map { it.id to it.label }
                        options.forEachIndexed { i, (id, label) ->
                            if (i > 0) IosDivider()
                            IosRow(label, onClick = { vm.selectSim(id) }) {
                                if (simId == id) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = ios.accent)
                            }
                        }
                    }
                    sims.size == 1 -> IosRow(sims[0].label, icon = Icons.Filled.SimCard, iconColor = ios.green) {
                        Text("Only SIM", color = ios.secondaryLabel, fontSize = 16.sp)
                    }
                    else -> IosRow(if (readiness.phoneState) "No SIM found" else "Default SIM", icon = Icons.Filled.SimCard, iconColor = ios.gray)
                }
            }

            if (SystemSettings.isXiaomi) {
                SectionCard("Background", padded = false, footer = "Stops your phone from closing OfflinePay.") {
                    IosRow("Autostart", icon = Icons.Filled.Autorenew, iconColor = ios.green, chevron = true, onClick = {
                        SystemSettings.autostart(ctx)
                    })
                    IosDivider(60.dp)
                    IosRow("Battery saver", icon = Icons.Filled.BatteryChargingFull, iconColor = ios.orange, chevron = true, onClick = {
                        SystemSettings.battery(ctx)
                    }) {
                        Text("No restrictions", color = ios.secondaryLabel, fontSize = 15.sp)
                    }
                }
            }

            SectionCard("Appearance") {
                val modes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                IosSegmented(
                    options = modes.map { it.second },
                    selected = modes.indexOfFirst { it.first == theme }.coerceAtLeast(0),
                    onSelect = { vm.setTheme(modes[it].first) },
                )
            }

            SectionCard("Support", padded = false) {
                IosRow("Donate", icon = Icons.Filled.Favorite, iconColor = Color(0xFFFF2D55), chevron = true, onClick = {
                    Links.open(ctx, Links.DONATE)
                })
                IosDivider(60.dp)
                IosRow("Share OfflinePay", icon = Icons.Filled.Share, iconColor = ios.accent, chevron = true, onClick = {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "OfflinePay")
                        .putExtra(Intent.EXTRA_TEXT, "Pay with UPI even without internet using OfflinePay: ${Links.SITE}")
                    ctx.startActivity(Intent.createChooser(send, "Share OfflinePay"))
                })
            }

            SectionCard("About", padded = false, footer = "Not affiliated with NPCI or any bank.") {
                IosRow("Privacy Policy", icon = Icons.Filled.PrivacyTip, iconColor = ios.blue, chevron = true, onClick = {
                    Links.open(ctx, Links.PRIVACY)
                })
                IosDivider(60.dp)
                IosRow("Terms of Use", icon = Icons.AutoMirrored.Filled.Article, iconColor = ios.gray, chevron = true, onClick = {
                    Links.open(ctx, Links.TERMS)
                })
                IosDivider(60.dp)
                IosRow("Version", icon = Icons.Filled.Info, iconColor = ios.gray) {
                    Text(BuildConfig.VERSION_NAME, color = ios.secondaryLabel, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (repair) {
        IosAlert(
            title = "Restart Accessibility",
            message = "Your phone stopped OfflinePay in the background. Allow Autostart, set Battery saver to No restrictions, then turn OfflinePay off and on in Accessibility.",
            actions = listOfNotNull(
                if (SystemSettings.isXiaomi) AlertAction("Allow Autostart") { SystemSettings.autostart(ctx) } else null,
                if (SystemSettings.isXiaomi) AlertAction("Battery Saver") { SystemSettings.battery(ctx) } else null,
                AlertAction("Open Accessibility", bold = true) {
                    repair = false
                    SystemSettings.accessibility(ctx)
                },
                AlertAction("Cancel") { repair = false },
            ),
            onDismiss = { repair = false },
        )
    }
}
