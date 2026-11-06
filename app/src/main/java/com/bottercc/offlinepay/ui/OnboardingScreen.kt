package com.bottercc.offlinepay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.R
import com.bottercc.offlinepay.ui.theme.LocalIos
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector?, val color: Color?, val title: String, val body: String)

/** First-run introduction. The last slide links the Terms and Privacy Policy and asks the user to agree. */
@Composable
fun OnboardingScreen(onAgree: () -> Unit) {
    val ios = LocalIos.current
    val slides = listOf(
        Slide(null, null, "Welcome to OfflinePay", "Pay with UPI even when there's no internet. All you need is mobile signal."),
        Slide(Icons.Filled.QrCodeScanner, ios.green, "Pay anyone", "Send money to a mobile number, UPI ID or bank account, or scan any UPI QR code."),
        Slide(Icons.Filled.AccountBalanceWallet, ios.teal, "Balance and history", "Check your bank balance and see your recent transactions in seconds."),
        Slide(Icons.Filled.Lock, ios.indigo, "Private and secure", "Your UPI PIN is never saved, and your payment history stays on your phone."),
        Slide(Icons.Filled.SignalCellularAlt, ios.accent, "Ready to go", "OfflinePay works through NPCI's *99# service. Your operator may charge up to ₹0.50 per session."),
    )
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(ios.groupedBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
            if (!last) {
                Text(
                    "Skip",
                    color = ios.accent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .pressable(scale = 1f, dim = 0.4f) { scope.launch { pager.animateScrollToPage(slides.lastIndex) } }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            val s = slides[page]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (s.icon == null) {
                    Image(
                        painterResource(R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(22.dp)),
                    )
                } else {
                    IconSquircle(s.icon, s.color ?: ios.accent, 72.dp, 18.dp)
                }
                Spacer(Modifier.height(28.dp))
                Text(s.title, color = ios.label, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(s.body, color = ios.secondaryLabel, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        }

        // Page dots; the current one stretches into a pill.
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            slides.indices.forEach { i ->
                val on = i == pager.currentPage
                val w by animateDpAsState(if (on) 20.dp else 7.dp, label = "dotWidth")
                val c by animateColorAsState(if (on) ios.accent else ios.separator, label = "dotColor")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = w, height = 7.dp)
                        .background(c, RoundedCornerShape(50)),
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            if (last) {
                val link = TextLinkStyles(SpanStyle(color = ios.accent, fontWeight = FontWeight.Medium))
                Text(
                    buildAnnotatedString {
                        append("By continuing, you agree to the ")
                        withLink(LinkAnnotation.Url(Links.TERMS, link)) { append("Terms of Use") }
                        append(" and ")
                        withLink(LinkAnnotation.Url(Links.PRIVACY, link)) { append("Privacy Policy") }
                        append(".")
                    },
                    color = ios.secondaryLabel,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
                IosButton("Agree & Continue", onClick = onAgree)
            } else {
                IosButton("Next", onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } })
            }
        }
    }
}
