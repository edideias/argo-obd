package br.com.edideias.argoobd

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date

fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

@Composable
fun ViagemScreen(m: Manager) {
    var refuel by remember { mutableStateOf("") }
    var earn by remember { mutableStateOf(if (m.cfg.earnKm > 0f) Fmt.num(m.cfg.earnKm.toDouble(), 2) else "") }
    var msg by remember { mutableStateOf("") }
    Live { _ ->
        val t = m.trip
        val cfg = m.cfg
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label("Viagem atual")
                        T(Fmt.num(t.dist, 1) + " km", 40.sp, C.TEXT, FontWeight.Light)
                        T("${Fmt.time(t.secs.toLong())} · média ${Fmt.num(t.avgSpeed(), 0)} km/h · máx ${Fmt.num(t.maxSpd, 0)} km/h", 12.sp, C.DIM)
                    }
                    ScoreRing(t.ecoScore(), Modifier.size(104.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Consumo médio", Fmt.num(t.kmPerL(), 1), "km/l", barColor = C.TEAL, bar = t.kmPerL()?.let { (it / 20.0).toFloat() })
                Metric("Custo por km", Fmt.num(t.costPerKm(), 2), "R$")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Combustível gasto", Fmt.num(t.liters, 2), "L")
                Metric("Custo da viagem", Fmt.num(t.cost, 2), "R$")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Arrancadas fortes", "${t.hardAcc}", "")
                Metric("Freadas fortes", "${t.hardBrk}", "")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Parado (ponto morto)", if (t.secs > 5) Fmt.num(t.idleSecs / t.secs * 100, 0) else "—", "%")
                Metric("Giro alto (>3500)", if (t.secs > 5) Fmt.num(t.hiRpmSecs / t.secs * 100, 0) else "—", "%")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Encerrar e salvar") {
                    t.saveToHistory(cfg.fuelName())
                    t.reset()
                    msg = "Viagem salva no histórico."
                }
                GhostButton("Zerar sem salvar") { t.reset(); msg = "Viagem zerada." }
            }
            if (msg.isNotEmpty()) T(msg, 12.sp, C.TEAL)

            Section("Quanto sobra por km (99/Uber)")
            Panel {
                T("Digite quanto você ganha por km rodado (já descontando o app). O app mostra o que sobra depois do combustível.", 12.sp, C.DIM)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = earn, onValueChange = { earn = it }, singleLine = true,
                    label = { T("Ganho bruto por km (R$)", 12.sp, C.DIM) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                )
                val e = parseNum(earn)
                val c = t.costPerKm() ?: (if (cfg.lifeDist > 5f) (cfg.lifeLiters / cfg.lifeDist * cfg.price()) else null)
                if (e != null && c != null) {
                    cfg.earnKm = e.toFloat()
                    Spacer(Modifier.height(8.dp))
                    KeyValue("Combustível por km", Fmt.money(c))
                    KeyValue("Sobra por km", Fmt.money(e - c), if (e - c > 0) C.GOOD else C.BAD)
                    KeyValue("Combustível é", Fmt.num(c / e * 100, 0) + "% do ganho")
                }
            }

            Section("Calibrar o consumo")
            Panel {
                T("O app estima o consumo pela ECU. Para ficar exato: zere a viagem no posto, rode, encha o tanque de novo e digite os litros que a bomba marcou.", 12.sp, C.DIM)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = refuel, onValueChange = { refuel = it }, singleLine = true,
                    label = { T("Litros abastecidos (bomba)", 12.sp, C.DIM) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                )
                val real = parseNum(refuel)
                if (real != null && t.liters > 0.5 && real > 0.5) {
                    val nc = cfg.calib * real / t.liters
                    Spacer(Modifier.height(6.dp))
                    T("O app estimou ${Fmt.num(t.liters, 2)} L; a bomba marcou ${Fmt.num(real, 2)} L. Novo fator: ${Fmt.num(nc, 3)}.", 12.sp, C.TEXT)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Aplicar calibração") {
                        cfg.calib = nc.toFloat().coerceIn(0.4f, 2.5f)
                        refuel = ""
                        msg = "Calibrado. Fator atual: " + Fmt.num(cfg.calib.toDouble(), 3)
                    }
                } else {
                    T("Fator atual: ${Fmt.num(cfg.calib.toDouble(), 3)}", 12.sp, C.DIM)
                }
            }

            Section("Manutenção (pelo hodômetro do app)")
            Panel {
                val items = cfg.maint()
                T("Total registrado pelo app: ${Fmt.num(cfg.odo.toDouble(), 0)} km. Acerte o valor em Ajustes para bater com o painel do carro.", 12.sp, C.DIM)
                Spacer(Modifier.height(8.dp))
                items.forEachIndexed { i, it ->
                    val done = cfg.odo - it.lastKm
                    val left = it.intervalKm - done
                    val col = when {
                        left < 0 -> C.BAD
                        left < it.intervalKm * 0.15 -> C.WARN
                        else -> C.GOOD
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(it.name, 14.sp, C.TEXT, FontWeight.Medium, Modifier.weight(1f))
                        T(if (left < 0) "atrasado ${Fmt.num(-left, 0)} km" else "faltam ${Fmt.num(left, 0)} km", 12.sp, col, FontWeight.SemiBold)
                    }
                    Bar((done / it.intervalKm).toFloat().coerceIn(0f, 1f), col, Modifier.padding(vertical = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("Feito agora") {
                            it.lastKm = cfg.odo.toDouble()
                            cfg.saveMaint(items)
                        }
                        GhostButton("− 1000") {
                            it.intervalKm = (it.intervalKm - 1000).coerceAtLeast(1000)
                            cfg.saveMaint(items)
                        }
                        GhostButton("+ 1000") {
                            it.intervalKm += 1000
                            cfg.saveMaint(items)
                        }
                    }
                    if (i < items.size - 1) Spacer(Modifier.height(12.dp))
                }
            }

            val hist = t.history()
            Section("Histórico de viagens")
            if (hist.isEmpty()) Panel { Empty("Nenhuma viagem salva ainda.") }
            for (h in hist.take(30)) {
                val d = h.optDouble("dist")
                val l = h.optDouble("liters")
                Panel(pad = 12.dp) {
                    Row {
                        T(DateFormat.format("dd/MM HH:mm", Date(h.optLong("start"))).toString(), 13.sp, C.TEXT, FontWeight.SemiBold, Modifier.weight(1f))
                        T(h.optString("fuel"), 11.sp, C.DIM)
                    }
                    Spacer(Modifier.height(2.dp))
                    T(
                        "${Fmt.num(d, 1)} km · ${Fmt.time(h.optDouble("secs").toLong())} · " +
                            (if (l > 0.02) Fmt.num(d / l, 1) + " km/l · " else "") +
                            Fmt.money(h.optDouble("cost")) +
                            (h.optInt("eco", -1).takeIf { it >= 0 }?.let { " · nota $it" } ?: ""),
                        12.sp, C.DIM
                    )
                }
            }
            if (hist.isNotEmpty()) GhostButton("Limpar histórico", color = C.BAD) { t.clearHistory(); msg = "Histórico limpo." }
            Spacer(Modifier.height(12.dp))
        }
    }
}
