package br.com.edideias.argoobd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class AjustesPage(private val a: MainActivity) : Page {
    private val scroll = ScrollView(a)
    override val view: View get() = scroll
    private val col = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(30)) }

    private val connInfo = a.tv("", 13f, C.DIM)
    private val devBox = a.vbox()
    private val demoBtn: TextView
    private val keepBtn: TextView
    private val fuelBtns = ArrayList<TextView>()
    private val etCustom = edit("Etanol na mistura (%)", InputType.TYPE_CLASS_NUMBER)
    private val etGas = edit("Preço da gasolina (R$/L)", dec())
    private val etEth = edit("Preço do etanol (R$/L)", dec())
    private val etDisp = edit("Cilindrada (litros)", dec())
    private val etVe = edit("Eficiência volumétrica (0,60 a 1,00)", dec())
    private val etCal = edit("Fator de calibração do consumo", dec())
    private var shownDevices = ""

    private fun dec() = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

    private fun edit(hint: String, type: Int): EditText = EditText(a).apply {
        this.hint = hint
        inputType = type
        setTextColor(C.TEXT)
        setHintTextColor(C.DIM)
        textSize = 15f
        background = roundBg(C.CARD2, a.dpf(10f))
        setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10))
    }

    init {
        val cfg = a.cfg

        col.addView(a.sectionTitle("Conexão com o leitor"))
        col.addView(connInfo, lp(MATCH, WRAP, b = a.dp(6)))
        col.addView(devBox)
        val r = a.hbox()
        r.addView(a.btn("Abrir pareamento", false) {
            a.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }, lp(0, WRAP, 1f, r = a.dp(5)))
        r.addView(a.btn("Desconectar", false) { a.disconnect() }, lp(0, WRAP, 1f, l = a.dp(5)))
        col.addView(r, lp(MATCH, WRAP, t = a.dp(8)))
        col.addView(a.tv("Primeiro pareie o leitor nas configurações do Android (senha 1234 ou 0000), depois toque no nome dele aqui.", 12f, C.DIM).apply {
            setPadding(a.dp(2), a.dp(8), 0, 0)
        })

        demoBtn = a.btn("", false) {
            cfg.demo = !cfg.demo
            if (cfg.demo) a.connect(true) else a.disconnect()
            refresh()
        }
        col.addView(demoBtn, lp(MATCH, WRAP, t = a.dp(12)))

        col.addView(a.sectionTitle("Combustível"))
        val fr = a.hbox()
        val labels = listOf("Gasolina", "Etanol", "Mistura")
        labels.forEachIndexed { i, n ->
            val b = a.btn(n, false) {
                cfg.fuelType = i
                refresh()
            }
            fuelBtns.add(b)
            fr.addView(b, lp(0, WRAP, 1f, r = a.dp(if (i < 2) 6 else 0)))
        }
        col.addView(fr)
        col.addView(a.tv("Gasolina comum é considerada E30 (30% etanol). Se a ECU informar o etanol real, o app usa o valor dela.", 12f, C.DIM).apply {
            setPadding(a.dp(2), a.dp(6), 0, a.dp(6))
        })
        for (e in listOf(etCustom, etGas, etEth)) col.addView(e, lp(MATCH, WRAP, t = a.dp(6)))

        col.addView(a.sectionTitle("Motor (para estimar o consumo)"))
        col.addView(a.tv("Argo Drive 1.0 Firefly: cilindrada 0,999 L. Se o consumo parecer alto ou baixo demais, use \"Calibrar km/l\" na aba VIAGEM.", 12f, C.DIM).apply {
            setPadding(a.dp(2), 0, 0, a.dp(6))
        })
        for (e in listOf(etDisp, etVe, etCal)) col.addView(e, lp(MATCH, WRAP, t = a.dp(6)))

        keepBtn = a.btn("", false) {
            cfg.keepOn = !cfg.keepOn
            a.applyKeepOn()
            refresh()
        }
        col.addView(keepBtn, lp(MATCH, WRAP, t = a.dp(14)))

        col.addView(a.btn("Salvar") { save() }, lp(MATCH, WRAP, t = a.dp(14)))
        col.addView(a.tv("Argo OBD " + versionName(), 12f, C.DIM).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, a.dp(16), 0, 0)
        }, lp(MATCH, WRAP))
        loadFields()
        scroll.addView(col)
    }

    private fun versionName(): String = try {
        a.packageManager.getPackageInfo(a.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }

    private fun loadFields() {
        val c = a.cfg
        etCustom.setText(c.customEth.toString())
        etGas.setText(Fmt.num(c.priceGas.toDouble(), 2))
        etEth.setText(Fmt.num(c.priceEth.toDouble(), 2))
        etDisp.setText(Fmt.num(c.disp.toDouble(), 3))
        etVe.setText(Fmt.num(c.ve.toDouble(), 2))
        etCal.setText(Fmt.num(c.calib.toDouble(), 3))
    }

    private fun num(e: EditText): Double? = e.text.toString().trim().replace(',', '.').toDoubleOrNull()

    private fun save() {
        val c = a.cfg
        val eth = num(etCustom)?.toInt()
        val g = num(etGas)
        val et = num(etEth)
        val d = num(etDisp)
        val ve = num(etVe)
        val cal = num(etCal)
        if (eth == null || g == null || et == null || d == null || ve == null || cal == null ||
            eth !in 0..100 || d < 0.3 || d > 6.0 || ve < 0.4 || ve > 1.2 || cal < 0.3 || cal > 3.0
        ) {
            Toast.makeText(a, "Confira os valores digitados", Toast.LENGTH_LONG).show()
            return
        }
        c.customEth = eth
        c.priceGas = g.toFloat()
        c.priceEth = et.toFloat()
        c.disp = d.toFloat()
        c.ve = ve.toFloat()
        c.calib = cal.toFloat()
        Toast.makeText(a, "Ajustes salvos", Toast.LENGTH_SHORT).show()
        refresh()
    }

    @SuppressLint("MissingPermission")
    private fun buildDevices() {
        val c = a.cfg
        devBox.removeAllViews()
        val list = try {
            val bm = a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
        if (list.isEmpty()) {
            val needPerm = Build.VERSION.SDK_INT >= 31 && !a.hasBtPerm()
            devBox.addView(a.tv(
                if (needPerm) "Permissão de Bluetooth pendente."
                else "Nenhum aparelho pareado. Use \"Abrir pareamento\" e pareie o ELM327 (OBDII).",
                13f, C.WARN
            ))
            if (needPerm) devBox.addView(a.btn("Permitir Bluetooth") { a.ensurePerm() }, lp(MATCH, WRAP, t = a.dp(6)))
            return
        }
        for (d in list) {
            val sel = d.address == c.lastDevice && !c.demo
            val name = (d.name ?: "Sem nome") + "\n" + d.address
            val b = a.tv(name, 14f, if (sel) C.BG else C.TEXT, true).apply {
                background = roundBg(if (sel) C.ACCENT else C.CARD, a.dpf(12f))
                setPadding(a.dp(14), a.dp(10), a.dp(14), a.dp(10))
                setOnClickListener {
                    c.lastDevice = d.address
                    c.lastDeviceName = d.name ?: ""
                    c.demo = false
                    a.connect(false)
                    refresh()
                }
            }
            devBox.addView(b, lp(MATCH, WRAP, b = a.dp(6)))
        }
    }

    override fun refresh() {
        val c = a.cfg
        val m = a.mgr
        connInfo.text = "Estado: " + m.status + if (m.demoMode && m.isRunning()) "  (demonstração)" else ""

        val key = c.lastDevice + c.demo + a.hasBtPerm()
        if (key != shownDevices) {
            shownDevices = key
            buildDevices()
        }

        demoBtn.text = if (c.demo) "Modo demonstração: LIGADO (toque para desligar)" else "Modo demonstração: desligado (carro simulado)"
        demoBtn.background = roundBg(if (c.demo) C.WARN else C.CARD2, a.dpf(12f))
        demoBtn.setTextColor(if (c.demo) C.BG else C.TEXT)

        keepBtn.text = if (c.keepOn) "Manter tela ligada: SIM" else "Manter tela ligada: não"
        keepBtn.background = roundBg(if (c.keepOn) C.ACCENT else C.CARD2, a.dpf(12f))
        keepBtn.setTextColor(if (c.keepOn) C.BG else C.TEXT)

        fuelBtns.forEachIndexed { i, b ->
            val s = c.fuelType == i
            b.background = roundBg(if (s) C.ACCENT else C.CARD2, a.dpf(12f))
            b.setTextColor(if (s) C.BG else C.TEXT)
        }
        etCustom.visibility = if (c.fuelType == 2) View.VISIBLE else View.GONE
    }
}
