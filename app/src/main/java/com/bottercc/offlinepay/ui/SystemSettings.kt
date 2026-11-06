package com.bottercc.offlinepay.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Shortcuts into the system screens that keep the accessibility service alive. */
object SystemSettings {
    /** Xiaomi, Redmi and POCO phones (MIUI / HyperOS) kill background services unless Autostart is allowed. */
    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase().let { it == "xiaomi" || it == "redmi" || it == "poco" }

    fun accessibility(ctx: Context) = open(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun appInfo(ctx: Context) =
        open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))

    /** HyperOS "Autostart" list; falls back to the app's info page on other phones. */
    fun autostart(ctx: Context) {
        val miui = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        if (!open(ctx, miui)) appInfo(ctx)
    }

    /** Per-app battery saver page ("No restrictions" on HyperOS); falls back to the app's info page. */
    fun battery(ctx: Context) {
        val miui = Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
            .putExtra("package_name", ctx.packageName)
            .putExtra("package_label", "OfflinePay")
        if (!open(ctx, miui)) appInfo(ctx)
    }

    private fun open(ctx: Context, intent: Intent): Boolean =
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
