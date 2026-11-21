package app.caravan

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.caravan.data.Demo
import app.caravan.domain.Lang
import app.caravan.trip.TripService
import app.caravan.ui.CaravanTheme
import app.caravan.ui.Onboarding
import app.caravan.ui.Root
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var then: (() -> Unit)? = null
    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { then?.invoke(); then = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG)); return
        }
        Lang.ru = resources.configuration.locales[0].language == "ru"
        current = WeakReference(this)
        val c = container
        if (BuildConfig.DEMO) {
            Demo.tab = intent.getIntExtra("tab", -1); Demo.open = intent.getStringExtra("open")
            c.scope.launch { Demo.seedIfEmpty(this@MainActivity, c.game) }
        }
        TripService.resume(this)
        go(intent)
        c.game.ensureKnown()
        if (c.game.now.trip == null) c.locateOnce()
        setContent {
            val s by c.game.state.collectAsStateWithLifecycle()
            val sys = isSystemInDarkTheme()
            val dark = if (BuildConfig.SHOTS) false else when (s.theme) { "dark" -> true; "light" -> false; else -> sys }
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t), navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t))
            }
            CaravanTheme(dark) { if (s.onboarded) Root(s) else Onboarding() }
        }
    }

    /** Debug builds: `am start -n app.caravan.dev/app.caravan.MainActivity --es sim "55.75,37.60"` drives there. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        go(intent)
        if (!BuildConfig.DEBUG || BuildConfig.DEMO) return
        // debug: `--es trip car|bus|metro` starts a ride, `--ez end true` ends it, `--ei gold 5000` adds coins
        intent.getStringExtra("trip")?.let { app.caravan.trip.TripService.start(this, app.caravan.domain.Ride.of(it)); return }
        if (intent.getBooleanExtra("help", false)) { app.caravan.ui.Help.open = true; return }
        if (intent.getBooleanExtra("end", false)) { app.caravan.trip.TripService.stop(this); return }
        intent.getIntExtra("gold", 0).takeIf { it != 0 }?.let { g -> container.game.write { it.copy(gold = it.gold + g) }; return }
        val sim = intent.getStringExtra("sim") ?: return
        val (la, lo) = sim.split(',').map { it.trim().toDouble() }
        container.scope.launch { app.caravan.data.Sim.driveTo(container.game, la, lo) }
    }

    /** From the «caravan is ready» notification or the quick-settings tile: set off at once. */
    private fun go(intent: android.content.Intent?) {
        val ride = intent?.getStringExtra(app.caravan.trip.Auto.EXTRA_GO) ?: return
        intent.removeExtra(app.caravan.trip.Auto.EXTRA_GO)
        val c = container
        if (!c.game.now.onboarded || c.game.now.trip != null) return
        Manifests.askLocation { if (c.feed.permitted()) TripService.start(this, app.caravan.domain.Ride.of(ride)) }
    }

    companion object {
        private var current: WeakReference<MainActivity>? = null

        /** Asks for [list] (those not granted yet), then runs [done] whatever the answer. */
        fun ask(list: List<String>, done: () -> Unit) {
            val a = current?.get() ?: return done()
            val missing = list.filter { androidx.core.content.ContextCompat.checkSelfPermission(a, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) return done()
            a.then = done
            a.perms.launch(missing.toTypedArray())
        }
    }
}

/** Switches the app language: "" follows the phone, "ru" or "en" pins it. */
fun setLanguage(tag: String) = AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

object Manifests {
    fun askLocation(done: () -> Unit) {
        val list = buildList {
            add(android.Manifest.permission.ACCESS_FINE_LOCATION); add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        MainActivity.ask(list, done)
    }
}
