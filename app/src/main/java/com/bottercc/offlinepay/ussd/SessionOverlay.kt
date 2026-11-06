package com.bottercc.offlinepay.ussd

import android.accessibilityservice.AccessibilityService
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.bottercc.offlinepay.R
import com.bottercc.offlinepay.data.Store

/**
 * Full-screen accessibility overlay drawn above the USSD dialog while OfflinePay is answering it,
 * so the user sees progress instead of the raw menus. It steps aside whenever the user has to
 * type into the dialog themselves.
 */
class SessionOverlay(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val removeLater = Runnable { remove() }

    private var root: View? = null
    private lateinit var progress: ProgressBar
    private lateinit var badge: TextView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var step: TextView
    private lateinit var cancel: TextView

    private val dark: Boolean
        get() = when (Store(service).theme) {
            "light" -> false
            "dark" -> true
            else -> (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    private val brand = Color.parseColor("#FC5502")
    private val bg get() = if (dark) Color.BLACK else Color.WHITE
    private val fg get() = if (dark) Color.parseColor("#EDEDED") else Color.parseColor("#151515")
    private val muted get() = if (dark) Color.parseColor("#A8A8A8") else Color.parseColor("#666666")

    fun render(s: SessionState) {
        when (s.status) {
            SessionStatus.DIALING, SessionStatus.RUNNING -> {
                handler.removeCallbacks(removeLater)
                show()
                progress.visibility = View.VISIBLE
                badge.visibility = View.GONE
                cancel.visibility = View.VISIBLE
                title.text = s.title
                subtitle.text = s.payeeName?.let { "Paying $it" } ?: "No internet needed"
                step.text = s.hint ?: "Dialing *99#…"
            }
            // The user has to type into the dialog (e.g. their PIN), so get out of the way.
            SessionStatus.WAITING_USER -> remove()
            SessionStatus.SUCCESS, SessionStatus.FAILED, SessionStatus.UNKNOWN -> {
                if (root == null) return
                progress.visibility = View.GONE
                badge.visibility = View.VISIBLE
                cancel.visibility = View.GONE
                val ok = s.status == SessionStatus.SUCCESS
                badge.text = if (ok) "✓" else if (s.status == SessionStatus.FAILED) "!" else "?"
                badge.background = circle(if (ok) Color.parseColor("#1E9E3A") else if (s.status == SessionStatus.FAILED) Color.parseColor("#D32F2F") else Color.parseColor("#F2A100"))
                title.text = if (ok) "Done" else if (s.status == SessionStatus.FAILED) "Didn't go through" else "Finished"
                step.text = ""
                // Stay up while the dialog is being closed underneath, then reveal the app.
                handler.removeCallbacks(removeLater)
                handler.postDelayed(removeLater, 1400)
            }
            SessionStatus.CANCELLED -> {
                handler.removeCallbacks(removeLater)
                handler.postDelayed(removeLater, 500)
            }
            SessionStatus.IDLE -> remove()
        }
    }

    fun remove() {
        handler.removeCallbacks(removeLater)
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    private fun show() {
        if (root != null) return
        val view = build()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        runCatching { wm.addView(view, lp) }.onSuccess {
            root = view
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun build(): View {
        val ctx = service
        val frame = FrameLayout(ctx).apply {
            setBackgroundColor(bg)
            isClickable = true // swallow touches so the dialog underneath can't be tapped by accident
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)
        }
        val logo = ImageView(ctx).apply {
            setImageResource(R.drawable.app_logo)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, dp(24).toFloat())
            }
            clipToOutline = true
        }
        col.addView(logo, LinearLayout.LayoutParams(dp(96), dp(96)).apply { bottomMargin = dp(36) })

        val status = FrameLayout(ctx)
        progress = ProgressBar(ctx).apply { indeterminateTintList = ColorStateList.valueOf(brand) }
        badge = TextView(ctx).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            typeface = Typeface.DEFAULT_BOLD
            visibility = View.GONE
        }
        status.addView(progress, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER))
        status.addView(badge, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER))
        col.addView(status, LinearLayout.LayoutParams(dp(72), dp(72)).apply { bottomMargin = dp(20) })

        title = text(22f, fg, bold = true)
        subtitle = text(15f, muted)
        step = text(14f, brand)
        col.addView(title)
        col.addView(subtitle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        col.addView(step, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(18) })

        frame.addView(col, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))

        cancel = text(15f, fg).apply {
            text = "Cancel"
            setPadding(dp(28), dp(12), dp(28), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), muted)
            }
            setOnClickListener { UssdController.cancel() }
        }
        frame.addView(cancel, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM).apply { bottomMargin = dp(64) })

        val footer = text(12f, muted).apply { text = "OfflinePay is talking to your bank over *99#" }
        frame.addView(footer, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM).apply { bottomMargin = dp(32) })
        return frame
    }

    private fun text(sp: Float, color: Int, bold: Boolean = false) = TextView(service).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun dp(v: Int) = (v * service.resources.displayMetrics.density).toInt()
}
