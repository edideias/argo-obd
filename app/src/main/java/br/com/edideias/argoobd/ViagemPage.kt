package br.com.edideias.argoobd

import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViagemPage(private val a: MainActivity) : Page {
    private val m get() = a.mgr
    private val scroll = ScrollView(a)
    override val view: View get() = scroll
    private val col = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(20)) }

    private val tDist = Tile(a, "DISTÂNCIA", "km")
    private val tTime = Tile(a, "TEMPO", "")
    private val tAvgSpd = Tile(a, "VELOCIDADE MÉDIA", "km/h")
    private val tMax = Tile(a, "VELOCIDADE MÁXIMA", "km/h")
    private val tLit = Tile(a, "COMBUSTÍVEL USADO", "litros (estimado)")
    private val tKmL = Tile(a, "MÉDIA", "km/l")
    private val tCost = Tile(a, "CUSTO", "")
    private val tCostKm = Tile(a, "CUSTO POR KM", "R$/km")
    private val method = a.tv("", 12f, C.DIM)
    private val histCol = a.vbox()
    private var histKey = ""

    init {
        fun row(x: Tile, y: Tile) {
            val r = a.hbox()
            r.addView(x, lp(0, WRAP, 1f, r = a.dp(5)))
            r.addView(y, lp(0, WRAP, 1f, l = a.dp(5)))
            col.addView(r, lp(MATCH, WRAP, b = a.dp(10)))
        }
        row(tDist, tTime)
        row(tKmL, tCostKm)
        row(tLit, tCost)
        row(tAvgSpd, tMax)
        col.addView(method, lp(MATCH, WRAP, b = a.dp(10)))

        val b = a.hbox()
        b.addView(a.btn("Salvar e zerar") { saveAndReset() }, lp(0, WRAP, 1f, r = a.dp(5)))
        b.addView(a.btn("Calibrar km/l", false) { calibrate() }, lp(0, WRAP, 1f, l = a.dp(5)))
        col.addView(b)

        col.addView(a.sectionTitle("Viagens salvas"))
        col.addView(histCol)
        scroll.addView(col)
    }

    private fun toast(s: String) = Toast.makeText(a, s, Toast.LENGTH_LONG).show()

    private fun saveAndReset() {
        if (m.trip.dist < 0.05) {
            toast("Ainda não há viagem para salvar")
            return
        }
        m.trip.saveToHistory(a.cfg.fuelName())
        m.trip.reset()
        histKey = ""
        toast("Viagem salva")
    }

    private fun calibrate() {
        val t = m.trip
        if (t.liters < 0.3 || t.dist < 5) {
            AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Calibrar o consumo")
                .setMessage("Como calibrar: zere a viagem logo depois de encher o tanque até o talo. Rode normalmente. No próximo abastecimento, encha de novo até o talo, volte aqui e informe quantos litros entraram. O app ajusta o fator para bater com a bomba.\n\nPrecisa de pelo menos 5 km rodados para calibrar.")
                .setPositiveButton("Entendi", null)
                .show()
            return
        }
        val et = EditText(a).apply {
            hint = "litros que entraram na bomba"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(C.TEXT)
            setHintTextColor(C.DIM)
        }
        AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Litros abastecidos")
            .setMessage("O app estimou %s L em %s km. Informe quanto a bomba colocou de verdade.".format(Fmt.num(t.liters, 2), Fmt.num(t.dist, 1)))
            .setView(et)
            .setPositiveButton("Calibrar") { _, _ ->
                val real = et.text.toString().replace(',', '.').toDoubleOrNull()
                if (real == null || real <= 0.0) {
                    toast("Valor inválido")
                } else {
                    val ratio = (real / t.liters).coerceIn(0.4, 2.5)
                    a.cfg.calib = (a.cfg.calib * ratio).toFloat()
                    toast("Fator de calibração: " + Fmt.num(a.cfg.calib.toDouble(), 3) + " (vale para as próximas viagens)")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun refresh() {
        val t = m.trip
        tDist.set(Fmt.num(t.dist, 1))
        tTime.set(Fmt.time(t.secs.toLong()))
        tAvgSpd.set(Fmt.num(t.avgSpeed(), 0))
        tMax.set(Fmt.num(t.maxSpd, 0))
        tLit.set(Fmt.num(t.liters, 2))
        tKmL.set(Fmt.num(t.kmPerL(), 1))
        tCost.set(Fmt.money(t.cost))
        tCost.setUnit(a.cfg.fuelName() + " a " + Fmt.money(a.cfg.price()) + "/L")
        tCostKm.set(Fmt.num(t.costPerKm(), 2))
        method.text = "Consumo calculado por: " + Calc.method(m.values) +
            ". Fator de calibração " + Fmt.num(a.cfg.calib.toDouble(), 3) + "."

        val hist = t.history()
        val key = hist.size.toString() + (hist.firstOrNull()?.optLong("start") ?: 0L)
        if (key != histKey) {
            histKey = key
            histCol.removeAllViews()
            if (hist.isEmpty()) {
                histCol.addView(a.tv("Nenhuma viagem salva ainda.", 13f, C.DIM))
            }
            val df = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
            for (o in hist) {
                val dist = o.optDouble("dist")
                val lit = o.optDouble("liters")
                val cost = o.optDouble("cost")
                val c = a.card(12)
                c.addView(a.tv(df.format(Date(o.optLong("start"))) + "  •  " + Fmt.num(dist, 1) + " km  •  " + Fmt.time(o.optDouble("secs").toLong()), 14f, C.TEXT, true))
                val kml = if (lit > 0.02) dist / lit else null
                val ckm = if (dist > 0.1) cost / dist else null
                c.addView(a.tv(Fmt.num(kml, 1) + " km/l  •  " + Fmt.money(cost) + "  •  " + Fmt.num(ckm, 2) + " R$/km  •  " + o.optString("fuel"), 12f, C.DIM).apply {
                    setPadding(0, a.dp(4), 0, 0)
                })
                histCol.addView(c, lp(MATCH, WRAP, b = a.dp(8)))
            }
        }
    }
}
