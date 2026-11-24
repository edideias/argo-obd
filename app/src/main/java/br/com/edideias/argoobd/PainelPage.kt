package br.com.edideias.argoobd

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PainelPage(private val a: MainActivity) : Page {
    private val m get() = a.mgr
    private val scroll = ScrollView(a)
    override val view: View get() = scroll

    private val banner: TextView = a.tv("", 14f, C.BG, true)
    private val offCard = a.card(16)
    private val gauge = ArcGauge(a)

    private val tKmL = Tile(a, "CONSUMO AGORA", "km/l")
    private val tAvg = Tile(a, "MÉDIA DA VIAGEM", "km/l")
    private val tCostKm = Tile(a, "CUSTO POR KM", "R$/km")
    private val tTrip = Tile(a, "VIAGEM", "km")
    private val tTemp = Tile(a, "ÁGUA", "°C")
    private val tBat = Tile(a, "BATERIA", "V")
    private val tLoad = Tile(a, "CARGA DO MOTOR", "%")
    private val tThr = Tile(a, "ACELERADOR", "%")
    private val tAir = Tile(a, "AR ADMITIDO", "°C")
    private val tFuel = Tile(a, "COMBUSTÍVEL NO TANQUE", "%")

    init {
        val col = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(20)) }

        banner.gravity = Gravity.CENTER
        banner.background = roundBg(C.WARN, a.dpf(12f))
        banner.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10))
        banner.visibility = View.GONE
        col.addView(banner, lp(MATCH, WRAP, b = a.dp(10)))

        offCard.addView(a.tv("Sem leitor conectado", 18f, C.TEXT, true))
        offCard.addView(a.tv("Plugue o leitor na tomada OBD do carro, pareie nas configurações do Android e escolha ele em AJUSTES. Ou ative o modo demonstração para ver o app funcionando.", 13f, C.DIM).apply {
            setPadding(0, a.dp(6), 0, a.dp(10))
        })
        offCard.addView(a.btn("Abrir ajustes") { a.showPage(4) })
        col.addView(offCard, lp(MATCH, WRAP, b = a.dp(10)))

        col.addView(gauge, lp(MATCH, WRAP))

        fun row(x: Tile, y: Tile) {
            val r = a.hbox()
            r.addView(x, lp(0, WRAP, 1f, r = a.dp(5)))
            r.addView(y, lp(0, WRAP, 1f, l = a.dp(5)))
            col.addView(r, lp(MATCH, WRAP, t = a.dp(10)))
        }
        row(tKmL, tAvg)
        row(tCostKm, tTrip)
        row(tTemp, tBat)
        row(tLoad, tThr)
        row(tAir, tFuel)

        scroll.addView(col)
    }

    private fun f(v: Double?, d: Int) = Fmt.num(v, d)

    override fun refresh() {
        val on = m.connected
        offCard.visibility = if (on) View.GONE else View.VISIBLE

        if (on && m.milOn) {
            banner.text = "Luz de injeção acesa — ${m.dtcCount} falha(s). Veja a aba FALHAS."
            banner.visibility = View.VISIBLE
        } else if (on && !m.ecuOk) {
            banner.text = m.status
            banner.visibility = View.VISIBLE
        } else {
            banner.visibility = View.GONE
        }

        val rpm = m.fresh(0x0C)
        val spd = m.fresh(0x0D)
        gauge.value = (rpm ?: 0.0).toFloat()
        gauge.big = if (spd != null) Fmt.num(spd, 0) else "—"
        gauge.sub = if (rpm != null) Fmt.num(rpm, 0) + " rpm" else ""

        val kml = m.instKmL
        val lh = m.instLh
        val avg = m.trip.kmPerL()
        if (kml != null) {
            tKmL.set(f(kml, 1), kmlColor(kml, avg))
            tKmL.setUnit("km/l")
        } else if (lh != null && rpm != null && rpm > 300) {
            tKmL.set(f(lh, 1), C.TEXT)
            tKmL.setUnit("L/h (parado)")
        } else {
            tKmL.set("—")
            tKmL.setUnit("km/l")
        }
        tAvg.set(f(avg, 1))
        tCostKm.set(f(m.trip.costPerKm(), 2))
        tCostKm.setUnit("R$/km")
        tTrip.set(f(m.trip.dist, 1))
        tTrip.setUnit("km  •  " + Fmt.money(m.trip.cost))

        val t = m.fresh(0x05, 8000)
        tTemp.set(f(t, 0), when {
            t == null -> C.TEXT
            t < 60 -> C.COLD
            t > 108 -> C.BAD
            t > 100 -> C.WARN
            else -> C.GOOD
        })
        tTemp.setUnit(when {
            t == null -> "°C"
            t < 60 -> "°C  •  motor frio"
            t > 108 -> "°C  •  SUPERAQUECENDO"
            else -> "°C"
        })

        val v = m.fresh(Pids.VOLT, 12000) ?: m.fresh(0x42, 8000)
        tBat.set(f(v, 1), when {
            v == null -> C.TEXT
            v < 12.0 -> C.BAD
            (rpm ?: 0.0) > 500 && v < 13.0 -> C.WARN
            v > 15.0 -> C.BAD
            else -> C.GOOD
        })
        tLoad.set(f(m.fresh(0x04, 8000), 0))
        tThr.set(f(m.fresh(0x11, 8000), 0))
        tAir.set(f(m.fresh(0x0F, 12000), 0))
        val fuel = m.fresh(0x2F, 60000)
        tFuel.set(f(fuel, 0), when {
            fuel == null -> C.TEXT
            fuel < 12 -> C.BAD
            fuel < 25 -> C.WARN
            else -> C.TEXT
        })
    }

    private fun kmlColor(k: Double, avg: Double?): Int {
        if (avg == null) return C.TEXT
        return when {
            k >= avg * 1.1 -> C.GOOD
            k < avg * 0.6 -> C.BAD
            k < avg * 0.85 -> C.WARN
            else -> C.TEXT
        }
    }
}
