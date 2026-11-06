package com.bottercc.offlinepay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottercc.offlinepay.data.Parse
import com.bottercc.offlinepay.data.StatementItem
import com.bottercc.offlinepay.data.Txn
import com.bottercc.offlinepay.ui.theme.LocalIos
import com.bottercc.offlinepay.ussd.SessionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tap handling with iOS-like feedback instead of the Android ripple: the element shrinks and dims
 * slightly while pressed and springs back on release.
 */
@Composable
fun Modifier.pressable(enabled: Boolean = true, scale: Float = 0.96f, dim: Float = 0.7f, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scale else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "pressScale")
    val a by animateFloatAsState(if (pressed) dim else 1f, tween(120), label = "pressAlpha")
    return this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = a }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** iOS navigation bar: "‹ Back" on the left, centred title, optional trailing action. */
@Composable
fun SubTopBar(title: String, onBack: () -> Unit, backLabel: String = "Back", trailing: @Composable RowScope.() -> Unit = {}) {
    val ios = LocalIos.current
    Box(
        Modifier
            .fillMaxWidth()
            .background(ios.groupedBg)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .height(50.dp),
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(10.dp))
                .pressable(scale = 1f, dim = 0.4f, onClick = onBack)
                .padding(start = 2.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = ios.accent, modifier = Modifier.size(32.dp))
            Text(backLabel, color = ios.accent, fontSize = 17.sp)
        }
        Text(title, Modifier.align(Alignment.Center), color = ios.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** iOS inset-grouped section: small uppercase header, rounded card, optional footer. */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    footer: String? = null,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ios = LocalIos.current
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title.uppercase(),
                color = ios.secondaryLabel,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ios.card)
                .then(if (padded) Modifier.padding(16.dp) else Modifier),
            content = content,
        )
        if (footer != null) {
            Text(footer, color = ios.secondaryLabel, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
        }
    }
}

/** A row inside a [SectionCard] (use padded = false on the card). */
@Composable
fun IosRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val ios = LocalIos.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val highlight by animateColorAsState(if (pressed) ios.fill else Color.Transparent, tween(if (pressed) 0 else 250), label = "rowHighlight")
    Row(
        Modifier
            .fillMaxWidth()
            .background(highlight)
            .then(if (onClick != null) Modifier.clickable(interactionSource = source, indication = null, onClick = onClick) else Modifier)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconSquircle(icon, iconColor ?: ios.accent, 30.dp, 7.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = ios.label, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = ios.secondaryLabel, fontSize = 13.sp)
        }
        trailing()
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ios.separator)
    }
}

@Composable
fun IosDivider(start: Dp = 16.dp) {
    HorizontalDivider(Modifier.padding(start = start), thickness = 0.5.dp, color = LocalIos.current.separator)
}

/** Coloured rounded square with a white glyph, like an iOS Settings icon. */
@Composable
fun IconSquircle(icon: ImageVector, color: Color, size: Dp = 50.dp, radius: Dp = 14.dp) {
    Box(
        Modifier
            .size(size)
            .background(color, RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun ActionTile(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .pressable(scale = 0.88f, dim = 0.8f, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconSquircle(icon, color)
        Spacer(Modifier.height(7.dp))
        Text(label, color = LocalIos.current.label, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
    }
}

/** Rounded, filled text field in the iOS style. */
@Composable
fun IosField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    prefix: String? = null,
    error: String? = null,
    hint: String? = null,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = TextStyle(fontSize = 17.sp),
) {
    val ios = LocalIos.current
    Column(modifier.fillMaxWidth()) {
        if (label != null) Text(label, color = ios.secondaryLabel, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboard,
            textStyle = textStyle.copy(color = ios.label),
            cursorBrush = SolidColor(ios.accent),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ios.fill)
                        .then(if (error != null) Modifier.background(ios.red.copy(alpha = 0.08f)) else Modifier)
                        .heightIn(min = 50.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix != null) {
                        Text(prefix, style = textStyle, color = ios.secondaryLabel)
                        Spacer(Modifier.width(6.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = textStyle, color = ios.secondaryLabel.copy(alpha = 0.7f))
                        inner()
                    }
                    if (value.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "Clear",
                            tint = ios.secondaryLabel.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onValueChange("") },
                        )
                    }
                }
            },
        )
        val note = error ?: hint
        if (note != null) {
            Text(note, color = if (error != null) ios.red else ios.secondaryLabel, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
    }
}

/** Large filled button (iOS "prominent" style). */
@Composable
fun IosButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color? = null) {
    val ios = LocalIos.current
    val bg = color ?: ios.accent
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressable(enabled = enabled, scale = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(animateColorAsState(if (enabled) bg else bg.copy(alpha = 0.4f), label = "buttonBg").value),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Plain tinted button (iOS "gray" style). */
@Composable
fun IosSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIos.current
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressable(scale = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(ios.fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = ios.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** iOS segmented control; the thumb slides to the selected segment. */
@Composable
fun IosSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ios = LocalIos.current
    val thumb = if (ios.card == Color.White) Color.White else Color(0xFF636366)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ios.fill)
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.78f, stiffness = 500f), label = "segmentThumb")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .height(32.dp)
                .background(thumb, RoundedCornerShape(8.dp)),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = ios.label, fontSize = 13.sp, fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

/** Small capsule chip, e.g. quick amounts. */
@Composable
fun IosChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val ios = LocalIos.current
    val bg by animateColorAsState(if (selected) ios.accent else ios.fill, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else ios.label, tween(220), label = "chipFg")
    Text(
        text,
        color = fg,
        fontSize = 15.sp,
        modifier = Modifier
            .pressable(scale = 0.92f, onClick = onClick)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
fun Avatar(text: String, size: Int = 44) {
    val ios = LocalIos.current
    val palette = listOf(ios.blue, ios.green, ios.orange, ios.purple, ios.teal, ios.indigo, ios.red)
    val initials = text.filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { "?" }
    val color = palette[(text.lowercase().hashCode() and 0x7fffffff) % palette.size]
    Box(
        Modifier
            .size(size.dp)
            .background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = color, fontWeight = FontWeight.SemiBold, fontSize = (size / 2.8).sp)
    }
}

fun statusLabel(s: SessionStatus): String = when (s) {
    SessionStatus.SUCCESS -> "Success"
    SessionStatus.FAILED -> "Failed"
    SessionStatus.CANCELLED -> "Cancelled"
    else -> "Check SMS"
}

private val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

/** Who a payment went to: the name the bank resolved, or the UPI ID / number. */
val Txn.payee: String get() = title.removePrefix("Paid ").removePrefix("Requested from ")

private fun Txn.whenText() = dateFormat.format(Date(time)).replace("AM", "am").replace("PM", "pm")

@Composable
fun TxnRow(t: Txn, onClick: () -> Unit = {}) {
    val ios = LocalIos.current
    val color = when (t.status) {
        SessionStatus.SUCCESS -> ios.green
        SessionStatus.FAILED -> ios.red
        else -> ios.orange
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(t.payee, 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.payee, color = ios.label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.whenText(), color = ios.secondaryLabel, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (t.amount != null) {
                    Text((if (t.kind == "send") "−" else "") + Parse.rupees(t.amount), color = ios.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(statusLabel(t.status), color = color, fontSize = 12.sp)
            }
        }
    }
}

data class AlertAction(val label: String, val bold: Boolean = false, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * iOS-style modal: blurs what's behind it (Android 12+, plain dim before that) and shows a
 * centred translucent card.
 */
@Composable
fun IosDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val ios = LocalIos.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            window?.let { w ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    w.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    w.attributes = w.attributes.apply { blurBehindRadius = 64 }
                    w.setDimAmount(0.25f)
                } else {
                    w.setDimAmount(0.5f)
                }
            }
        }
        // Pops in slightly larger and settles, like an iOS alert.
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 420f)) }
        Column(
            Modifier
                .graphicsLayer {
                    val p = appear.value
                    scaleX = 1.15f - 0.15f * p
                    scaleY = 1.15f - 0.15f * p
                    alpha = p.coerceIn(0f, 1f)
                }
                .width(290.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ios.card.copy(alpha = 0.96f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** iOS alert: title, message, then one or two buttons separated by hairlines. */
@Composable
fun IosAlert(title: String, message: String?, actions: List<AlertAction>, onDismiss: () -> Unit) {
    val ios = LocalIos.current
    IosDialog(onDismiss) {
        Column(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, color = ios.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (message != null) Text(message, color = ios.label, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
        }
        AlertButtons(actions)
    }
}

@Composable
private fun AlertButtons(actions: List<AlertAction>) {
    val ios = LocalIos.current
    @Composable
    fun Button(a: AlertAction, modifier: Modifier) {
        Box(
            modifier
                .height(46.dp)
                .pressable(scale = 1f, dim = 0.45f, onClick = a.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                a.label,
                color = if (a.destructive) ios.red else ios.accent,
                fontSize = 17.sp,
                fontWeight = if (a.bold) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = ios.separator)
    if (actions.size == 2) {
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            Button(actions[0], Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).fillMaxHeight().background(ios.separator))
            Button(actions[1], Modifier.weight(1f))
        }
    } else {
        actions.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = ios.separator)
            Button(a, Modifier.fillMaxWidth())
        }
    }
}

/** iOS-style alert with the details of one payment. */
@Composable
fun PaymentDialog(t: Txn, onDismiss: () -> Unit) {
    val ios = LocalIos.current
    IosDialog(onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(t.amount?.let { Parse.rupees(it).removeSuffix(".00") } ?: "Payment", color = ios.label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("To: ${t.payee}", color = ios.label, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text("Time: ${t.whenText()}", color = ios.label, fontSize = 16.sp)
            Parse.refId(t.message)?.let { Text("UTR / Ref: $it", color = ios.label, fontSize = 16.sp) }
            if (t.status != SessionStatus.SUCCESS) {
                val color = if (t.status == SessionStatus.FAILED) ios.red else ios.orange
                Text("Status: ${statusLabel(t.status)}", color = color, fontSize = 16.sp)
                if (t.status == SessionStatus.FAILED) {
                    Text(t.message, color = ios.secondaryLabel, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
        AlertButtons(listOf(AlertAction("Close", bold = true, onClick = onDismiss)))
    }
}

@Composable
fun StatementRow(item: StatementItem) {
    val ios = LocalIos.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(item.party, 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.party, color = ios.label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text((if (item.credit) "Received · " else "Sent · ") + item.date, color = ios.secondaryLabel, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (item.credit) "+" else "−") + Parse.rupees(item.amount),
            color = if (item.credit) ios.green else ios.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
}

@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalIos.current.secondaryLabel, fontSize = 13.sp, modifier = modifier)
}

/** Plain-coloured page background used by every screen. */
@Composable
fun pageColor() = MaterialTheme.colorScheme.background
