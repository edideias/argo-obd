package br.com.edideias.argoobd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Hub {
    private var mgr: Manager? = null
    @Synchronized
    fun get(ctx: Context): Manager {
        val app = ctx.applicationContext
        return mgr ?: Manager(app, Cfg(app)).also { mgr = it }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF05080D.toInt()
        window.navigationBarColor = 0xFF05080D.toInt()
        val m = Hub.get(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = C.BG, surface = C.BG, primary = C.TEAL)) {
                Root(m)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Hub.get(this).cfg.keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private fun hasBtPerm(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

private data class Tab(val icon: String, val label: String)

private val TABS = listOf(
    Tab("◉", "Painel"), Tab("⚙", "Oficina"), Tab("⏱", "Testes"),
    Tab("⛽", "Viagem"), Tab("⚠", "Falhas"), Tab("☰", "Ajustes")
)

@Composable
private fun Root(m: Manager) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var hud by remember { mutableStateOf(false) }
    var perm by remember { mutableStateOf(hasBtPerm(ctx)) }

    fun connect() {
        if (m.cfg.demo) m.start(null, true)
        else if (m.cfg.lastDevice.isNotEmpty() && hasBtPerm(ctx)) m.start(m.cfg.lastDevice, false)
        else m.status = if (m.cfg.lastDevice.isEmpty()) "Escolha o leitor em Ajustes" else "Permita o Bluetooth"
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        perm = hasBtPerm(ctx)
        if (perm && !m.isRunning()) connect()
    }
    fun askPerm() {
        if (Build.VERSION.SDK_INT >= 31) launcher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!perm) askPerm()
        if (!m.isRunning() && (m.cfg.demo || m.cfg.lastDevice.isNotEmpty())) connect()
    }

    BackHandler(enabled = hud || tab != 0) {
        if (hud) hud = false else tab = 0
    }

    if (hud) {
        HudScreen(m) { hud = false }
        return
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(C.BG2, C.BG)))) {
        TopBar(m)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> PainelScreen(m, onHud = { hud = true }, onGoto = { tab = it })
                1 -> OficinaScreen(m)
                2 -> TestesScreen(m)
                3 -> ViagemScreen(m)
                4 -> FalhasScreen(m)
                else -> AjustesScreen(m, onReconnect = { connect() }, hasPerm = perm, askPerm = { askPerm() })
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
private fun TopBar(m: Manager) {
    Live { _ ->
        val col = when {
            m.connected && m.ecuOk -> C.GOOD
            m.connected -> C.WARN
            m.isRunning() -> C.WARN
            else -> C.BAD
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dot(col, 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                T("ARGO OBD", 11.sp, C.TEAL, FontWeight.Bold)
                T(m.status + if (m.demoMode && m.connected) " · demonstração" else "", 12.sp, C.DIM, lines = 1)
            }
            val v = m.fresh(Pids.VOLT, 15000)
            if (v != null) Pill(Fmt.num(v, 1) + " V", C.SKY)
            if (m.milOn) {
                Spacer(Modifier.width(6.dp))
                Pill("MIL", C.BAD)
            }
        }
    }
}

@Composable
private fun BottomBar(sel: Int, onSel: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF080D14)).padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TABS.forEachIndexed { i, t ->
            val on = i == sel
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) C.TEAL.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { onSel(i) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                T(t.icon, 18.sp, if (on) C.TEAL else C.DIM)
                T(t.label, 10.sp, if (on) C.TEAL else C.DIM, if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}
