package br.com.edideias.argoobd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TestesScreen(m: Manager) {
    var sel by remember { mutableStateOf(0) }
    Live { _ ->
        val a = m.accel
        val spd = m.fresh(0x0D)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Panel(border = C.WARN.copy(alpha = 0.4f)) {
                T("Use só em pista fechada ou local seguro e permitido. Você é responsável pela direção.", 12.sp, C.WARN)
            }
            Panel(pad = 18.dp) {
                val t = if (a.active() || a.state == 3) a.test else Accel.tests[sel]
                Label(t.label)
                Spacer(Modifier.height(6.dp))
                val big = when (a.state) {
                    3 -> Fmt.num(a.result, 2)
                    2 -> Fmt.num(a.elapsed, 1)
                    else -> "0,0"
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    T(big, 72.sp, when {
                        a.state == 3 && a.record -> C.GOOD
                        a.state == 2 -> C.TEAL
                        else -> C.TEXT
                    }, FontWeight.Light)
                    T(" s", 20.sp, C.DIM, modifier = Modifier.padding(bottom = 14.dp))
                }
                val msg = when (a.state) {
                    1 -> if (t.brake) "Acelere até ${t.from} km/h e pise no freio."
                    else if (t.from == 0) "Pare o carro e arranque quando quiser. O cronômetro começa sozinho."
                    else "Chegue a ${t.from} km/h e acelere fundo."
                    2 -> "Em andamento · ${Fmt.num(spd, 0)} km/h · ${Fmt.num(a.dist, 0)} m"
                    3 -> (if (a.record) "Novo recorde! " else "") + (a.resultSpeed?.let { "Passou o marco a ${Fmt.num(it, 0)} km/h. " } ?: "") +
                        (if (t.brake) "Distância de frenagem: ${Fmt.num(a.dist, 0)} m." else "")
                    else -> "Escolha um teste e toque em Armar."
                }
                T(msg, 13.sp, if (a.state == 3 && a.record) C.GOOD else C.DIM)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (a.state == 0 || a.state == 3) {
                        PrimaryButton("Armar teste", enabled = m.connected) { a.arm(Accel.tests[sel]) }
                    } else {
                        GhostButton("Cancelar", color = C.BAD) { a.cancel() }
                    }
                }
                if (!m.connected) T("Conecte ao carro para cronometrar.", 12.sp, C.WARN, modifier = Modifier.padding(top = 8.dp))
            }
            Section("Testes")
            for ((i, t) in Accel.tests.withIndex()) {
                Panel(pad = 12.dp, border = if (i == sel) C.TEAL.copy(alpha = 0.6f) else C.LINE, onClick = { if (!a.active()) { sel = i; if (a.state == 3) a.cancel() } }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(t.label, 14.sp, C.TEXT, FontWeight.Medium, Modifier.weight(1f))
                        val best = m.cfg.bestTime(t.key)
                        T(if (best == null) "sem recorde" else "melhor " + Fmt.num(best, 2) + " s", 12.sp, if (best == null) C.DIM else C.TEAL)
                    }
                }
            }
            T("A velocidade vem da ECU, em passos de 1 km/h e ~5 leituras por segundo. A precisão é de décimos de segundo, boa para comparar antes e depois de uma mudança, não para competição.", 11.sp, C.DIM)
            Spacer(Modifier.height(12.dp))
        }
    }
}
