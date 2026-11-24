package br.com.edideias.argoobd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private fun tempColor(t: Double?): Color = when {
    t == null -> C.TEXT
    t < 60 -> C.COLD
    t < 100 -> C.GOOD
    t < 106 -> C.WARN
    else -> C.BAD
}

@Composable
fun PainelScreen(m: Manager, onHud: () -> Unit, onGoto: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    Live { tick ->
        val v = m.values
        val rpm = m.fresh(0x0C)
        val spd = m.fresh(0x0D)
        val ect = m.fresh(0x05)
        val volt = m.fresh(Pids.VOLT, 15000) ?: m.fresh(0x42, 15000)
        val load = m.fresh(0x04)
        val thr = m.fresh(0x11)
        val iat = m.fresh(0x0F)
        val fuelLvl = m.fresh(0x2F, 30000)
        val trip = m.trip
        val kmL = m.instKmL
        val lh = m.instLh
        val running = (rpm ?: 0.0) > 300
        val moving = (spd ?: 0.0) > 3

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tick >= 0 && m.alertNow.isNotEmpty()) {
                Panel(border = C.BAD.copy(alpha = 0.6f)) {
                    for (a in m.alertNow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(C.BAD)
                            Spacer(Modifier.width(8.dp))
                            T(a, 14.sp, C.BAD, FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (!m.connected && !m.demoMode) {
                Panel {
                    T("Leitor desconectado", 16.sp, C.WARN, FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    T("Escolha o seu leitor ELM327 em Ajustes e ligue a ignição do carro. Para ver como o app funciona, ligue o Modo demonstração.", 13.sp, C.DIM)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Abrir Ajustes") { onGoto(5) }
                }
            }

            Panel(pad = 8.dp) {
                RpmGauge(
                    rpm = rpm?.toFloat(), speed = spd?.toFloat(),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.05f),
                    sub = if (!m.ecuOk && m.connected) "ECU sem resposta" else ""
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    GhostButton("Modo HUD") { onHud() }
                    GhostButton("Personalizar") { custom = true }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val big = when {
                    !running -> "—"
                    moving && kmL != null -> Fmt.num(kmL, 1)
                    lh != null -> Fmt.num(lh, 1)
                    else -> "—"
                }
                val unit = if (moving && kmL != null) "km/l" else "L/h"
                Metric(
                    "Consumo agora", big, unit,
                    color = when {
                        !moving || kmL == null -> C.TEXT
                        kmL >= 12 -> C.GOOD
                        kmL >= 8 -> C.WARN
                        else -> C.BAD
                    },
                    bar = if (moving && kmL != null) (kmL / 20.0).toFloat() else null,
                    barColor = C.TEAL,
                    onClick = { onGoto(3) }
                )
                Metric(
                    "Média da viagem", Fmt.num(trip.kmPerL(), 1), "km/l",
                    bar = trip.kmPerL()?.let { (it / 20.0).toFloat() },
                    barColor = C.SKY,
                    onClick = { onGoto(3) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    "Custo por km", Fmt.money(trip.costPerKm()).replace("R$ ", ""), "R$/km",
                    barColor = C.WARN
                )
                Metric(
                    "Viagem", Fmt.num(trip.dist, 1), "km",
                    spark = null
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    "Água do motor", ect?.let { Fmt.num(it, 0) } ?: "—", "°C",
                    color = tempColor(ect),
                    bar = ect?.let { ((it - 20) / 100.0).toFloat() },
                    barColor = tempColor(ect)
                )
                Metric(
                    "Bateria", Fmt.num(volt, 1), "V",
                    color = when {
                        volt == null -> C.TEXT
                        running && volt < 13.2 -> C.WARN
                        !running && volt < 12.2 -> C.WARN
                        else -> C.TEXT
                    },
                    bar = volt?.let { ((it - 11.0) / 4.0).toFloat() },
                    barColor = C.GOOD
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Carga do motor", Fmt.num(load, 0), "%", bar = load?.let { (it / 100.0).toFloat() }, barColor = C.TEAL)
                Metric("Acelerador", Fmt.num(thr, 0), "%", bar = thr?.let { (it / 100.0).toFloat() }, barColor = C.SKY)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val liters = fuelLvl?.let { it * cfgTank(m) / 100.0 }
                val avg = trip.kmPerL() ?: if (m.cfg.lifeLiters > 1f) (m.cfg.lifeDist / m.cfg.lifeLiters).toDouble() else null
                val range = if (liters != null && avg != null) liters * avg else null
                Metric(
                    "Combustível", Fmt.num(fuelLvl, 0), "%",
                    bar = fuelLvl?.let { (it / 100.0).toFloat() },
                    barColor = if ((fuelLvl ?: 100.0) < 15) C.BAD else C.GOOD
                )
                Metric(
                    "Autonomia", Fmt.num(range, 0), "km",
                    color = C.TEXT
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Ar admitido", Fmt.num(iat, 0), "°C")
                val af = Calc.airFuel(v, m.cfg)
                Metric("Mistura (ar/comb.)", Fmt.num(af, 1), ": 1")
            }

            val extra = m.cfg.motorPids.filter { it in m.supported }
            if (extra.isNotEmpty()) {
                Section("Seus sensores")
                var i = 0
                while (i < extra.size) {
                    val a = extra[i]
                    val b = extra.getOrNull(i + 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric(Pids.nameOf(a), Fmt.pid(a, m.fresh(a, 20000)), Pids.unitOf(a), spark = m.history(a), barColor = C.SKY)
                        if (b != null) Metric(Pids.nameOf(b), Fmt.pid(b, m.fresh(b, 20000)), Pids.unitOf(b), spark = m.history(b), barColor = C.SKY)
                        else Spacer(Modifier.weight(1f))
                    }
                    i += 2
                }
            }

            if (m.demoMode) {
                T("Modo demonstração: os números são simulados.", 11.sp, C.WARN, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (custom) {
        Dialog(onDismissRequest = { custom = false }) {
            Panel(pad = 16.dp) {
                T("Sensores extras no Painel", 16.sp, C.TEXT, FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                T("Marque o que quiser ver, com gráfico. Só aparecem os que o seu carro responde.", 12.sp, C.DIM)
                Spacer(Modifier.height(8.dp))
                var sel by remember { mutableStateOf(m.cfg.motorPids.toSet()) }
                val opts = m.supported.filter { it !in setOf(0x0C, 0x0D, 0x05, 0x04, 0x11, 0x0F, 0x2F, 0x42) }
                Column(Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                    if (opts.isEmpty()) T("Conecte ao carro para ver as opções.", 13.sp, C.DIM)
                    for (p in opts) {
                        Row(
                            Modifier.fillMaxWidth().clickable { sel = if (p in sel) sel - p else sel + p },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = p in sel,
                                onCheckedChange = { sel = if (it) sel + p else sel - p },
                                colors = CheckboxDefaults.colors(checkedColor = C.TEAL, uncheckedColor = C.DIM)
                            )
                            T(Pids.nameOf(p), 13.sp, C.TEXT)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Salvar") {
                        m.cfg.motorPids = sel.toList().sorted()
                        custom = false
                    }
                    GhostButton("Cancelar") { custom = false }
                }
            }
        }
    }
}

private fun cfgTank(m: Manager): Double = m.cfg.tank.toDouble()

/** Modo HUD: velocidade enorme, espelhada para refletir no para-brisa. Toque para sair. */
@Composable
fun HudScreen(m: Manager, onExit: () -> Unit) {
    var mirror by remember { mutableStateOf(true) }
    Live { _ ->
        val spd = m.fresh(0x0D)
        val rpm = m.fresh(0x0C)
        val ect = m.fresh(0x05)
        val kmL = m.instKmL
        Box(
            Modifier.fillMaxSize().background(Color.Black).clickable { onExit() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.graphicsLayer { scaleX = if (mirror) -1f else 1f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                T(if (spd == null) "—" else Fmt.num(spd, 0), 190.sp, if ((spd ?: 0.0) > 110) C.BAD else C.TEXT, FontWeight.Light)
                T("km/h", 26.sp, C.DIM)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    T(if (rpm == null) "—" else Fmt.num(rpm, 0) + " rpm", 24.sp, C.TEAL, FontWeight.SemiBold)
                    T(if (ect == null) "—" else Fmt.num(ect, 0) + " °C", 24.sp, tempColor(ect), FontWeight.SemiBold)
                    T(if (kmL == null) "—" else Fmt.num(kmL, 1) + " km/l", 24.sp, C.GOOD, FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                for (a in m.alertNow) T(a, 20.sp, C.BAD, FontWeight.Bold)
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
                GhostButton(if (mirror) "Espelhado: ligado" else "Espelhado: desligado") { mirror = !mirror }
            }
        }
    }
}
