package com.bottercc.offlinepay.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bottercc.offlinepay.MainViewModel
import com.bottercc.offlinepay.data.PayDraft
import com.bottercc.offlinepay.ussd.PayMethod
import com.bottercc.offlinepay.ussd.UssdTask

// iOS push curve.
private val IosEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    fun toHome(n: NavHostController) = n.popBackStack("home", inclusive = false)

    fun launch(task: UssdTask) {
        val err = vm.start(task)
        if (err != null) {
            Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
            if (!vm.readiness.value.ready) nav.navigate("settings")
            return
        }
        nav.navigate("session") { popUpTo("home") }
    }

    fun begin(task: UssdTask) {
        vm.refresh()
        if (!vm.readiness.value.ready) {
            Toast.makeText(ctx, "Finish setup first so OfflinePay can run *99#.", Toast.LENGTH_LONG).show()
            nav.navigate("settings")
            return
        }
        if (task.needsPin) {
            vm.pending = task
            nav.navigate("pin")
        } else {
            launch(task)
        }
    }

    fun openPay(request: Boolean, method: PayMethod, draft: PayDraft?) {
        vm.draft = draft
        nav.navigate("pay/$request/${method.name}")
    }

    // Any session (however it was started) is shown on the session screen.
    val session by vm.session.collectAsState()
    LaunchedEffect(session.isActive) {
        if (session.isActive && nav.currentDestination?.route != "session") {
            nav.navigate("session") { popUpTo("home") }
        }
    }

    // Full-width slides both ways (forward: new page in from the right; back: page out to the right),
    // with a soft fade on the page underneath. They look smooth whichever page is drawn on top.
    val slide = tween<IntOffset>(420, easing = IosEasing)
    val fade = tween<Float>(420, easing = IosEasing)
    val start = remember { if (vm.onboarded) "home" else "onboarding" }
    NavHost(
        nav,
        startDestination = start,
        enterTransition = { slideIntoContainer(SlideDirection.Start, slide) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, slide) + fadeOut(fade, targetAlpha = 0.6f) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, slide) + fadeIn(fade, initialAlpha = 0.6f) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, slide) },
    ) {
        composable("onboarding") {
            OnboardingScreen(onAgree = {
                vm.finishOnboarding()
                nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
            })
        }
        composable("home") {
            HomeScreen(
                vm = vm,
                onScan = { nav.navigate("scan") },
                onPay = { request, method -> openPay(request, method, null) },
                onPayee = { p -> openPay(false, p.method, PayDraft.from(p)) },
                onBalance = { begin(UssdTask.CheckBalance()) },
                onLinkBank = { nav.navigate("link") },
                onStatement = { begin(UssdTask.Transactions) },
                onHistory = { nav.navigate("history") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("scan") {
            ScanScreen(
                onBack = { nav.popBackStack() },
                onScanned = { draft ->
                    vm.draft = draft
                    nav.navigate("pay/false/${PayMethod.UPI_ID.name}") { popUpTo("home") }
                },
            )
        }
        composable("pay/{request}/{method}") { entry ->
            val request = entry.arguments?.getString("request") == "true"
            val method = runCatching { PayMethod.valueOf(entry.arguments?.getString("method").orEmpty()) }.getOrDefault(PayMethod.MOBILE)
            val draft = remember { vm.draft }
            PayScreen(
                request = request,
                initialMethod = method,
                draft = draft,
                onBack = { nav.popBackStack() },
                onSubmit = { task, payee ->
                    vm.savePayee(payee)
                    begin(task)
                },
            )
        }
        composable("pin") {
            // Hold on to the task: vm.pending is cleared as soon as the session starts, while this
            // screen is still animating out. Reacting to that by navigating home used to throw away
            // the session screen (and the balance on it).
            val task = remember { vm.pending }
            if (task == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                val bank by vm.bank.collectAsState()
                PinScreen(
                    task = task,
                    bank = bank,
                    onBack = {
                        vm.pending = null
                        nav.popBackStack()
                    },
                    onPin = { pin -> launch(task.withPin(pin)) },
                )
            }
        }
        composable("session") {
            SessionScreen(
                vm = vm,
                onDone = {
                    vm.resetSession()
                    toHome(nav)
                },
                onRetry = {
                    val last = vm.lastTask()
                    vm.resetSession()
                    toHome(nav)
                    if (last != null) begin(last)
                },
            )
        }
        composable("link") {
            LinkBankScreen(onBack = { nav.popBackStack() }, onStart = { begin(UssdTask.LinkBank(it)) })
        }
        composable("history") { HistoryScreen(vm, onBack = { nav.popBackStack() }) }
        composable("settings") {
            SettingsScreen(vm, onBack = { nav.popBackStack() }, onAccessibility = { nav.navigate("accessibility") })
        }
        composable("accessibility") {
            AccessibilityScreen(
                onBack = { nav.popBackStack() },
                onOpenSettings = {
                    nav.popBackStack()
                    SystemSettings.accessibility(ctx)
                },
            )
        }
    }
}
