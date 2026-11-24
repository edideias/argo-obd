package br.com.edideias.argoobd

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Guarda o gerenciador vivo enquanto o processo existir. */
object Hub {
    private var m: Manager? = null
    fun get(a: Activity): Manager {
        val cur = m
        if (cur != null) return cur
        val n = Manager(a.applicationContext, Cfg(a.applicationContext))
        m = n
        return n
    }
}

class MainActivity : Activity() {
    lateinit var mgr: Manager
    val cfg: Cfg get() = mgr.cfg

    private val pages = ArrayList<Page>()
    private val tabViews = ArrayList<TextView>()
    private lateinit var content: FrameLayout
    private lateinit var dot: View
    private lateinit var statusTv: TextView
    private lateinit var demoTag: TextView
    private var cur = 0
    private val h = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            tick()
            h.postDelayed(this, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mgr = Hub.get(this)
        window.statusBarColor = C.BG
        window.navigationBarColor = C.BG

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C.BG)
        }

        // barra de estado
        val top = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(6))
        }
        dot = View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(C.DIM) } }
        top.addView(dot, lp(dp(10), dp(10), r = dp(8)))
        statusTv = tv("Desconectado", 13f, C.DIM)
        top.addView(statusTv, lp(0, WRAP, 1f))
        demoTag = tv("DEMO", 11f, C.BG, true).apply {
            background = roundBg(C.WARN, dpf(6f))
            setPadding(dp(8), dp(2), dp(8), dp(2))
            visibility = View.GONE
        }
        top.addView(demoTag, lp(WRAP, WRAP))
        root.addView(top, lp(MATCH, WRAP))

        content = FrameLayout(this)
        root.addView(content, lp(MATCH, 0, 1f))

        pages.add(PainelPage(this))
        pages.add(OficinaPage(this))
        pages.add(ViagemPage(this))
        pages.add(FalhasPage(this))
        pages.add(AjustesPage(this))

        // abas
        val bar = hbox().apply { setBackgroundColor(C.CARD) }
        val names = listOf("PAINEL", "OFICINA", "VIAGEM", "FALHAS", "AJUSTES")
        names.forEachIndexed { i, n ->
            val t = tv(n, 11f, C.DIM, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(14), 0, dp(14))
                setOnClickListener { showPage(i) }
            }
            tabViews.add(t)
            bar.addView(t, lp(0, WRAP, 1f))
        }
        root.addView(bar, lp(MATCH, WRAP))

        setContentView(root)
        applyKeepOn()
        showPage(0)

        // reconecta sozinho se já havia leitor escolhido
        if (!mgr.isRunning()) {
            if (cfg.demo) connect(true)
            else if (cfg.lastDevice.isNotEmpty() && hasBtPerm()) connect(false)
        }
    }

    fun showPage(i: Int) {
        cur = i
        content.removeAllViews()
        content.addView(pages[i].view)
        tabViews.forEachIndexed { idx, t -> t.setTextColor(if (idx == i) C.ACCENT else C.DIM) }
        mgr.liveMode = if (i == 1) 1 else 0
        pages[i].refresh()
    }

    private fun tick() {
        val s = mgr.status
        statusTv.text = s
        val color = when {
            mgr.connected && mgr.ecuOk -> C.GOOD
            mgr.isRunning() -> C.WARN
            else -> C.BAD
        }
        (dot.background as GradientDrawable).setColor(color)
        statusTv.setTextColor(if (mgr.connected && mgr.ecuOk) C.TEXT else C.DIM)
        demoTag.visibility = if (mgr.demoMode && mgr.isRunning()) View.VISIBLE else View.GONE
        pages[cur].refresh()
    }

    fun applyKeepOn() {
        if (cfg.keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // -------- conexão e permissão --------

    fun hasBtPerm(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun ensurePerm(): Boolean {
        if (hasBtPerm()) return true
        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 7)
        return false
    }

    private var pendingDemo = false

    fun connect(demo: Boolean) {
        if (demo) {
            mgr.start(null, true)
            return
        }
        if (!ensurePerm()) {
            pendingDemo = false
            return
        }
        mgr.start(cfg.lastDevice, false)
    }

    fun disconnect() {
        cfg.demo = false
        mgr.stop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (cfg.lastDevice.isNotEmpty()) connect(false)
        }
    }

    override fun onResume() {
        super.onResume()
        h.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(ticker)
        mgr.trip.save()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) mgr.stop()
    }
}
