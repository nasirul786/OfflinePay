package com.bottercc.offlinepay.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Public pages on the project website. */
object Links {
    const val SITE = "https://offlinepay.bottercc.com"
    const val TERMS = "$SITE/terms/"
    const val PRIVACY = "$SITE/privacy/"
    const val DONATE = "$SITE/donate"

    fun open(ctx: Context, url: String) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
