package com.bottercc.offlinepay.ussd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/** One active SIM; [id] is its subscription id. */
data class SimOption(val id: String, val label: String)

object Dialer {
    const val CODE = "*99#"

    fun hasCallPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun hasPhoneStatePermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun sims(ctx: Context): List<SimOption> {
        if (!hasPhoneStatePermission(ctx)) return emptyList()
        val sm = ctx.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        val subs = runCatching { sm.activeSubscriptionInfoList }.getOrNull().orEmpty()
        return subs.sortedBy { it.simSlotIndex }.map { info ->
            val carrier = (info.carrierName ?: info.displayName)?.toString()?.takeIf { it.isNotBlank() } ?: "SIM"
            SimOption(info.subscriptionId.toString(), "$carrier · SIM ${info.simSlotIndex + 1}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleFor(ctx: Context, subId: String): PhoneAccountHandle? {
        val id = subId.toIntOrNull() ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                ctx.getSystemService(TelephonyManager::class.java).createForSubscriptionId(id).phoneAccountHandle
            }.getOrNull()?.let { return it }
        }
        val tm = ctx.getSystemService(TelecomManager::class.java) ?: return null
        return runCatching { tm.callCapablePhoneAccounts.firstOrNull { it.id == subId } }.getOrNull()
    }

    /** Places the *99# call on the chosen SIM (or the system default). Returns an error message on failure. */
    @SuppressLint("MissingPermission")
    fun dial(ctx: Context, simId: String?): String? {
        if (!hasCallPermission(ctx)) return "Phone permission is needed to dial *99#."
        val tm = ctx.getSystemService(TelecomManager::class.java) ?: return "This device can't place calls."
        val extras = Bundle()
        if (simId != null && hasPhoneStatePermission(ctx)) {
            handleFor(ctx, simId)?.let { extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
        }
        return runCatching { tm.placeCall(Uri.fromParts("tel", CODE, null), extras) }
            .exceptionOrNull()?.let { "Couldn't dial *99#: ${it.message}" }
    }
}
