package app.caravan.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.caravan.BuildConfig
import app.caravan.R
import app.caravan.container
import app.caravan.data.State
import app.caravan.domain.Kind
import app.caravan.domain.Voronoi
import app.caravan.domain.tr
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

fun kindColor(k: Kind?): Int = when (k) {
    Kind.GARDEN -> 0xFF5C8F4A.toInt(); Kind.WATER -> 0xFF2E7FB0.toInt(); Kind.OLD -> 0xFFB4502F.toInt(); Kind.SCHOLAR -> 0xFF3A4C8E.toInt()
    Kind.FORGE -> 0xFF8A2E2E.toInt(); Kind.BAZAAR -> 0xFFD9A21E.toInt(); Kind.GATE -> 0xFF2A8C88.toInt(); Kind.HOMES -> 0xFFB58A5A.toInt()
    null -> 0xFF8C8577.toInt()
}

/** The city as quarters: known ones in their colours with crests, the rest pale and nameless until visited. */
@Composable
fun CityMap(s: State, open: (Over) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val live by c.game.live.collectAsState()
    val overlay = remember { QuarterOverlay(ctx.applicationContext) { open(Over.Quarter(it)) } }
    val map = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 9.0; maxZoomLevel = 18.0
            isHorizontalMapRepetitionEnabled = false; isVerticalMapRepetitionEnabled = false
            overlays.add(overlay)
            val here = s.quarters[s.here]
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(here?.lat ?: s.homeLat, here?.lon ?: s.homeLon))
        }
    }
    val dark = C.dark
    LaunchedEffect(dark) {
        map.overlayManager.tilesOverlay.setColorFilter(if (dark) nightTiles() else paperTiles())
        map.overlayManager.tilesOverlay.loadingBackgroundColor = if (dark) 0xFF141A2E.toInt() else 0xFFF3E6CC.toInt()
    }
    val cells = remember(s.quarters.keys) { Voronoi.cells(s.quarters.values) }
    overlay.state = s; overlay.cells = cells; overlay.fix = live.fix?.let { it.lat to it.lon }; overlay.accuracy = live.fix?.accuracy ?: 0f
    LaunchedEffect(s, live.fix) { map.invalidate() }
    // the first position after opening the map brings the view to the merchant
    var centred by remember { mutableStateOf(false) }
    LaunchedEffect(live.fix != null) {
        val f = live.fix ?: return@LaunchedEffect
        if (!centred) { centred = true; map.controller.animateTo(GeoPoint(f.lat, f.lon), 14.5, 600L) }
    }
    // the dot follows the phone while the map is open
    DisposableEffect(Unit) {
        // held even before permission: Feed.restart() starts it the moment location is allowed
        val held = !BuildConfig.DEMO
        if (held) c.feed.acquire()
        onDispose { if (held) c.feed.release() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> when (e) { Lifecycle.Event.ON_RESUME -> map.onResume(); Lifecycle.Event.ON_PAUSE -> map.onPause(); else -> Unit } }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); map.onPause(); map.onDetach() }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView({ map }, Modifier.fillMaxSize())
        val known = s.quarters.values.count { it.known }
        Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).framed(14.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pic("ic_map", Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(tr("Изведано кварталов: $known из ${s.quarters.size}", "Quarters explored: $known of ${s.quarters.size}"), style = T.smallB)
                Text(tr("Тапните квартал – что он делает и чего ждёт", "Tap a quarter – what it makes and wants"), style = T.tiny)
            }
        }
        HelpBtn(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp))
        // «where am I»: back to the phone's position, or to the current quarter while it's unknown
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).size(54.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(C.paper).border(1.5.dp, C.gold, androidx.compose.foundation.shape.CircleShape)
            .clickable {
                if (!c.feed.permitted()) { app.caravan.Manifests.askLocation { c.feed.restart() }; return@clickable }
                val f = live.fix ?: c.feed.last()
                val q = s.quarters[s.here]
                val p = f?.let { GeoPoint(it.lat, it.lon) } ?: q?.let { GeoPoint(it.lat, it.lon) }
                if (p != null) { map.controller.animateTo(p, 15.0, 600L) }
            }, contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.MyLocation, tr("Где я", "Where am I"), tint = C.acc, modifier = Modifier.size(26.dp))
        }
        if (BuildConfig.DEBUG && !BuildConfig.DEMO) Text("debug: long-press to drive", style = T.tiny, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
    if (BuildConfig.DEBUG && !BuildConfig.DEMO) overlay.onLong = { la, lo -> c.scope.launch { app.caravan.data.Sim.driveTo(c.game, la, lo) } }
}

class QuarterOverlay(private val ctx: Context, private val onTap: (String) -> Unit) : Overlay() {
    var state: State? = null
    var cells: Map<String, List<Pair<Double, Double>>> = emptyMap()
    var fix: Pair<Double, Double>? = null
    var accuracy = 0f
    var onLong: ((Double, Double) -> Unit)? = null
    private val crests = HashMap<String, Bitmap?>()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xCC8A6A2A.toInt() }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2B2017.toInt(); textAlign = Paint.Align.CENTER
        typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.philosopher_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
    private val halo = Paint(text).apply { style = Paint.Style.STROKE; color = 0xEEFBF3E1.toInt() }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB4502F.toInt() }
    private val p = Point()
    private val d = ctx.resources.displayMetrics.density

    private fun crest(id: String): Bitmap? = crests.getOrPut(id) {
        val res = ctx.resources.getIdentifier(id, "drawable", ctx.packageName)
        if (res == 0) null else ResourcesCompat.getDrawable(ctx.resources, res, null)?.let { dr ->
            val size = (34 * d).toInt()
            val h = (size * dr.intrinsicHeight / dr.intrinsicWidth.toFloat()).toInt()
            Bitmap.createBitmap(size, h, Bitmap.Config.ARGB_8888).also { b -> dr.setBounds(0, 0, size, h); dr.draw(Canvas(b)) }
        }
    }

    override fun draw(canvas: Canvas, map: MapView, shadow: Boolean) {
        if (shadow) return
        val s = state ?: return
        val pj = map.projection
        val zoom = map.zoomLevelDouble
        edge.strokeWidth = 1.4f * d
        for ((id, poly) in cells) {
            val q = s.quarters[id] ?: continue
            if (poly.size < 3) continue
            val path = Path()
            poly.forEachIndexed { i, (la, lo) -> pj.toPixels(GeoPoint(la, lo), p); if (i == 0) path.moveTo(p.x.toFloat(), p.y.toFloat()) else path.lineTo(p.x.toFloat(), p.y.toFloat()) }
            path.close()
            fill.color = kindColor(if (q.known) q.kind else null)
            fill.alpha = if (q.known) (if (id == s.here) 120 else 85) else 40
            canvas.drawPath(path, fill)
            canvas.drawPath(path, edge)
        }
        for (q in s.quarters.values) {
            pj.toPixels(GeoPoint(q.lat, q.lon), p)
            if (q.known) crest(q.kind!!.crest)?.let { b -> canvas.drawBitmap(b, p.x - b.width / 2f, p.y - b.height / 2f - 8 * d, null) }
            if (zoom >= 12.5 || q.known) {
                text.textSize = 12 * d; halo.textSize = 12 * d; halo.strokeWidth = 3 * d
                val y = p.y + (if (q.known) 22 else 4) * d
                text.alpha = if (q.known) 255 else 150
                canvas.drawText(q.name, p.x.toFloat(), y, halo); canvas.drawText(q.name, p.x.toFloat(), y, text)
            }
        }
        fix?.let { (la, lo) ->
            pj.toPixels(GeoPoint(la, lo), p)
            // how sure the phone is: a soft circle of the accuracy radius
            val r = pj.metersToPixels(accuracy.coerceAtMost(500f))
            if (r > 12 * d) { dot.color = 0x33B4502F; canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), r, dot) }
            dot.color = 0xFFFBF3E1.toInt(); canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 11 * d, dot)
            dot.color = 0xFFB4502F.toInt(); canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 7.5f * d, dot)
            // the merchant's cart stands on the dot
            crest("ride_car")?.let { b -> canvas.drawBitmap(b, p.x - b.width / 2f, p.y - b.height - 6 * d, null) }
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, map: MapView): Boolean {
        val s = state ?: return false
        val g = map.projection.fromPixels(e.x.toInt(), e.y.toInt())
        val q = app.caravan.domain.Quarters.at(s.quarters.values, g.latitude, g.longitude) ?: return false
        onTap(q.id)
        return true
    }

    override fun onLongPress(e: MotionEvent, map: MapView): Boolean {
        val f = onLong ?: return false
        val g = map.projection.fromPixels(e.x.toInt(), e.y.toInt())
        f(g.latitude, g.longitude)
        return true
    }
}

/** Night tiles: inverted, hue turned back so water stays blue, dimmed to indigo. */
fun nightTiles(): android.graphics.ColorMatrixColorFilter {
    val invert = android.graphics.ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
    val hue = android.graphics.ColorMatrix(floatArrayOf(-0.574f, 1.43f, 0.144f, 0f, 0f, 0.426f, 0.43f, 0.144f, 0f, 0f, 0.426f, 1.43f, -0.856f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
    invert.postConcat(hue)
    val dim = android.graphics.ColorMatrix(); dim.setScale(0.72f, 0.76f, 0.92f, 1f)
    invert.postConcat(dim)
    return android.graphics.ColorMatrixColorFilter(invert)
}

/** Day tiles washed to warm paper, so the quarters' colours carry the map. */
fun paperTiles(): android.graphics.ColorMatrixColorFilter {
    val m = android.graphics.ColorMatrix(); m.setSaturation(0f)
    val warm = android.graphics.ColorMatrix(floatArrayOf(.62f, 0f, 0f, 0f, 92f, 0f, .58f, 0f, 0f, 84f, 0f, 0f, .5f, 0f, 70f, 0f, 0f, 0f, 1f, 0f))
    m.postConcat(warm)
    return android.graphics.ColorMatrixColorFilter(m)
}
