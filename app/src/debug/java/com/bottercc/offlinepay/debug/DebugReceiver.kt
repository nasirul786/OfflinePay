package com.bottercc.offlinepay.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bottercc.offlinepay.data.Store
import com.bottercc.offlinepay.ussd.PayMethod
import com.bottercc.offlinepay.ussd.UssdController
import com.bottercc.offlinepay.ussd.UssdTask

/**
 * adb shell am broadcast -n com.bottercc.offlinepay/.debug.DebugReceiver --es cmd <cmd> [extras]
 *   cmd=probe --es replies 1,1           send fixed menu replies in order, then stop
 *   cmd=console                         dial and only record screens
 *   cmd=send  --es to x@upi --es amount 1 [--es method UPI_ID]   runs the send flow; stops at the PIN (no PIN is ever sent)
 *   cmd=balance                         runs the balance flow; stops at the PIN
 *   cmd=transactions | cmd=request ...  other flows
 *   cmd=cancel                          cancel the running session
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val method = intent.getStringExtra("method")?.let { PayMethod.valueOf(it) } ?: PayMethod.UPI_ID
        val to = intent.getStringExtra("to").orEmpty()
        val amount = intent.getStringExtra("amount") ?: "1"
        val remark = intent.getStringExtra("remark").orEmpty()
        val task = when (intent.getStringExtra("cmd")) {
            "console" -> UssdTask.Console
            "probe" -> UssdTask.Probe(intent.getStringExtra("replies").orEmpty().split(',').filter { it.isNotBlank() })
            "send" -> UssdTask.SendMoney(method, to, intent.getStringExtra("ifsc"), amount, remark, pin = null)
            "request" -> UssdTask.RequestMoney(method, to, amount, remark)
            "balance" -> UssdTask.CheckBalance(pin = null)
            "transactions" -> UssdTask.Transactions
            "cancel" -> {
                UssdController.cancel()
                return
            }
            else -> return
        }
        val err = UssdController.start(ctx, task, Store(ctx).simId)
        Log.i(TAG, "start $task -> ${err ?: "ok"}")
    }

    companion object {
        const val TAG = "OfflinePayUSSD"
    }
}
