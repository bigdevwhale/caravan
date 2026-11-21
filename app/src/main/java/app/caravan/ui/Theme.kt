package app.caravan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.caravan.R

/** Bazaar colours: cream paper, ink, terracotta for action, saffron for coins, indigo and turquoise from the tiles. */
class Pal(
    val dark: Boolean, val bg: Color, val paper: Color, val paper2: Color, val ink: Color, val mute: Color, val line: Color,
    val acc: Color, val accInk: Color, val gold: Color, val goldSoft: Color, val indigo: Color, val teal: Color, val ok: Color, val bad: Color, val shade: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF3E6CC), c(0xFBF3E1), c(0xF0E1C3), c(0x2B2017), c(0x7F6A52), c(0xD9C49E),
    c(0xB4502F), c(0xFFF8EC), c(0xC98A1E), c(0xF6E2B0), c(0x283A6E), c(0x2A8C88), c(0x3E7D4A), c(0xB3402A), Color(0x33201208))

val Dark = Pal(true, c(0x141A2E), c(0x1E2640), c(0x28304D), c(0xF0E4CC), c(0xA8A08E), c(0x39426A),
    c(0xD9714A), c(0x1B1208), c(0xE3AD48), c(0x3C3320), c(0x9AB0F0), c(0x52BDB6), c(0x7DC38B), c(0xEF7A5E), Color(0x66000000))

object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val paper get() = p.paper; val paper2 get() = p.paper2; val ink get() = p.ink; val mute get() = p.mute; val line get() = p.line
    val acc get() = p.acc; val accInk get() = p.accInk; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val indigo get() = p.indigo; val teal get() = p.teal
    val ok get() = p.ok; val bad get() = p.bad; val shade get() = p.shade
}

/** Philosopher for titles – a little of the East in its letters; Nunito for reading. */
val Phil = FontFamily(Font(R.font.philosopher, FontWeight.Normal), Font(R.font.philosopher_bold, FontWeight.Bold))
val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold))

object T {
    val big get() = TextStyle(fontFamily = Phil, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, color = C.ink)
    val title get() = TextStyle(fontFamily = Phil, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Phil, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Phil, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, color = C.ink)
    val huge get() = TextStyle(fontFamily = Phil, fontWeight = FontWeight.Bold, fontSize = 54.sp, lineHeight = 60.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Phil, fontWeight = FontWeight.Bold, fontSize = 18.sp)
}

@Composable
fun CaravanTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.paper, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.paper, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
