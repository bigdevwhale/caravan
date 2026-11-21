package app.caravan.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.caravan.domain.fmt

fun Modifier.tap(onClick: () -> Unit) = this.clickable(MutableInteractionSource(), null, onClick = onClick)

private val ids = HashMap<String, Int>()

@SuppressLint("DiscouragedApi")
@Composable
fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return ids.getOrPut(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

/** A drawable by name (the cut Codex art). */
@Composable
fun Pic(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, alpha: Float = 1f) {
    val id = drawableId(name)
    if (id != 0) Image(painterResource(id), null, modifier.alpha(alpha), contentScale = scale)
    else Box(modifier)
}

/** Paper with the double rule of a miniature's frame. */
fun Modifier.framed(r: Dp = 14.dp, fill: Color = C.paper, rule: Color = C.gold) = this
    .clip(RoundedCornerShape(r))
    .background(fill)
    .border(1.5.dp, rule.copy(alpha = 0.75f), RoundedCornerShape(r))
    .drawBehind {
        val i = 4.dp.toPx()
        drawRoundRect(rule.copy(alpha = 0.35f), topLeft = Offset(i, i), size = androidx.compose.ui.geometry.Size(size.width - 2 * i, size.height - 2 * i),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.toPx() - i), style = Stroke(1.dp.toPx()))
    }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, color: Color = C.acc, ink: Color = C.accInk, enabled: Boolean = true, icon: ImageVector? = null, small: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(if (small) 10.dp else 14.dp))
            .background(if (enabled) color else C.line)
            .border(1.dp, C.gold.copy(alpha = if (enabled) 0.8f else 0.3f), RoundedCornerShape(if (small) 10.dp else 14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 38.dp else 54.dp)
            .padding(horizontal = if (small) 12.dp else 18.dp, vertical = if (small) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (enabled) ink else C.mute, modifier = Modifier.size(if (small) 18.dp else 22.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = (if (small) T.btn.copy(fontSize = T.btn.fontSize * 0.85f) else T.btn).copy(color = if (enabled) ink else C.mute), textAlign = TextAlign.Center)
    }
}

@Composable
fun Ghost(text: String, modifier: Modifier = Modifier, color: Color = C.ink, onClick: () -> Unit) {
    Text(text, style = T.smallB.copy(color = color), modifier = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp))
}

/** Coins with the purse. */
@Composable
fun Coins(n: Int, style: TextStyle = T.num, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Pic("ic_purse", Modifier.size(size))
        Spacer(Modifier.width(4.dp))
        Text(fmt(n), style = style)
    }
}

/** A thin ornament: a line with a lozenge in the middle. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = C.gold) {
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp).height(10.dp).drawBehind {
        val y = size.height / 2; val cx = size.width / 2; val d = 5.dp.toPx()
        drawLine(color.copy(alpha = 0.6f), Offset(0f, y), Offset(cx - d * 2, y), 1.dp.toPx())
        drawLine(color.copy(alpha = 0.6f), Offset(cx + d * 2, y), Offset(size.width, y), 1.dp.toPx())
        drawPath(Path().apply { moveTo(cx, y - d); lineTo(cx + d, y); lineTo(cx, y + d); lineTo(cx - d, y); close() }, color)
    })
}

/** A bottom sheet over a dim scrim; scrolls when it has to. */
@Composable
fun Sheet(onDismiss: () -> Unit, scroll: Boolean = true, light: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val shown = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { shown.targetState = true }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(shown, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(if (light) 0x33000000 else 0x88000000)).tap(onDismiss))
        }
        AnimatedVisibility(shown, Modifier.align(Alignment.BottomCenter), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = if (light) 420.dp else 680.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(C.paper)
                    .border(1.5.dp, C.gold.copy(alpha = 0.7f), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .tap { }
                    .navigationBarsPadding()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(C.line))
                Spacer(Modifier.size(10.dp))
                content()
            }
        }
    }
}

/** A small pill with an icon and a number, for the top of a scene. */
@Composable
fun Pill(pic: String, text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(C.paper.copy(alpha = 0.92f)).border(1.dp, C.gold.copy(alpha = 0.7f), RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pic(pic, Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = T.smallB)
    }
}

@Composable
fun Meter(v: Float, modifier: Modifier = Modifier, color: Color = C.gold) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(C.line.copy(alpha = 0.6f))) {
        Box(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
fun Title(text: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text, style = T.title)
        if (sub != null) Text(sub, style = T.small)
    }
}

@Composable
fun Scroll(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), content = content)
