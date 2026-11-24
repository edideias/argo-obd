package br.com.edideias.argoobd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SUBS = listOf("Ao vivo", "Gráfico", "Saúde", "Cálculos", "Prontidão", "Modo 06", "Congelados", "Módulos", "Leitor", "Terminal", "Info")
private val SERIES: List<androidx.compose.ui.graphics.Color> get() = listOf(C.TEAL, C.WARN, C.SKY)

@Composable
fun OficinaScreen(m: Manager) {
    var sub by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf(listOf(0x0C)) }
    // Na oficina lemos todos os sensores; ao sair voltamos ao ritmo do Painel
    DisposableEffect(Unit) {
        m.liveMode = 1
        onDispose { m.liveMode = 0 }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SUBS.forEachIndexed { i, s -> Chip(s, sub == i) { sub = i } }
        }
        Column(Modifier.fillMaxSize()) {
            when (sub) {
                0 -> AoVivo(m, picked) { p ->
                    picked = if (p in picked) picked - p else (picked + p).takeLast(3)
                }
                1 -> Grafico(m, picked) { p -> picked = if (p in picked) picked - p else (picked + p).takeLast(3) }
                2 -> SaudeTab(m)
                3 -> CalculosTab(m)
                4 -> ProntidaoTab(m)
                5 -> Modo06Tab(m)
                6 -> CongeladosTab(m)
                7 -> ModulosTab(m)
                8 -> LeitorTab(m)
                9 -> TerminalTab(m)
                else -> InfoTab(m)
            }
        }
    }
}

@Composable
private fun AoVivo(m: Manager, picked: List<Int>, toggle: (Int) -> Unit) {
    var q by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    Live { _ ->
        val all = (listOf(Pids.VOLT) + m.supported).distinct()
        val list = all.filter { q.isBlank() || Pids.nameOf(it).contains(q, ignoreCase = true) }
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!m.logging) {
                    PrimaryButton("● Gravar CSV") { m.startLog(); msg = "Gravando…" }
                } else {
                    PrimaryButton("■ Parar e salvar") { msg = m.stopLog() }
                }
                GhostButton("Zerar mín/máx") { m.resetMinMax() }
            }
            if (m.logging) T("● gravando, mantenha o app aberto", 12.sp, C.BAD)
            else if (msg.isNotEmpty()) T(msg, 12.sp, C.TEAL)
            SearchField(q) { q = it }
            T("${m.supported.size} sensores aceitos pela ECU · toque para escolher até 3 no gráfico", 11.sp, C.DIM)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (list.isEmpty()) Empty("Nenhum sensor ainda. Conecte ao carro com a ignição ligada.")
                for (pid in list) {
                    val on = pid in picked
                    val idx = picked.indexOf(pid)
                    Panel(pad = 10.dp, border = if (on) SERIES[idx.coerceAtLeast(0)].copy(alpha = 0.7f) else C.LINE, onClick = { toggle(pid) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                T(Pids.nameOf(pid), 13.sp, C.TEXT, FontWeight.Medium)
                                val mn = m.mins[pid]
                                val mx = m.maxs[pid]
                                if (mn != null && mx != null)
                                    T("mín ${Fmt.pid(pid, mn)} · máx ${Fmt.pid(pid, mx)}", 10.sp, C.DIM)
                            }
                            Spark(m.history(pid), if (on) SERIES[idx.coerceAtLeast(0)] else C.FAINT, Modifier.width(70.dp).height(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(92.dp)) {
                                T(Fmt.pid(pid, m.fresh(pid, 20000)), 19.sp, C.TEAL, FontWeight.SemiBold, mono = false)
                                T(Pids.unitOf(pid), 10.sp, C.DIM)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SearchField(q: String, set: (String) -> Unit) {
    OutlinedTextField(
        value = q, onValueChange = set, singleLine = true,
        placeholder = { T("Buscar sensor…", 13.sp, C.DIM) },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors()
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = C.TEXT, unfocusedTextColor = C.TEXT,
    focusedBorderColor = C.TEAL, unfocusedBorderColor = C.LINE,
    cursorColor = C.TEAL
)

@Composable
private fun Grafico(m: Manager, picked: List<Int>, toggle: (Int) -> Unit) {
    Live { _ ->
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (picked.isEmpty()) Empty("Escolha sensores na aba Ao vivo para ver o gráfico.")
            else {
                Panel {
                    BigChart(picked.mapIndexed { i, p -> m.history(p) to SERIES[i % 3] }, Modifier.fillMaxWidth().height(260.dp))
                }
                picked.forEachIndexed { i, p ->
                    Panel(pad = 10.dp, border = SERIES[i % 3].copy(alpha = 0.6f)) {
                        T(Pids.nameOf(p), 13.sp, SERIES[i % 3], FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            T("agora ${Fmt.pid(p, m.fresh(p, 20000))} ${Pids.unitOf(p)}", 13.sp, C.TEXT)
                            T("mín ${Fmt.pid(p, m.mins[p])} · máx ${Fmt.pid(p, m.maxs[p])}", 12.sp, C.DIM)
                        }
                    }
                }
                T("Mostra os últimos ~240 pontos de cada sensor. Para guardar tudo, grave o CSV.", 11.sp, C.DIM)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SaudeTab(m: Manager) {
    Live { _ ->
        val checks = Health.run(m)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            T("Diagnóstico rápido, calculado com os dados ao vivo. Funciona melhor com o motor ligado e quente.", 12.sp, C.DIM)
            for (c in checks) {
                val col = when (c.level) { 0 -> C.GOOD; 1 -> C.WARN; 2 -> C.BAD; else -> C.DIM }
                Panel(pad = 12.dp, border = col.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(col)
                        Spacer(Modifier.width(8.dp))
                        T(c.title, 14.sp, C.TEXT, FontWeight.SemiBold, Modifier.weight(1f))
                        Pill(when (c.level) { 0 -> "ok"; 1 -> "atenção"; 2 -> "problema"; else -> "sem dado" }, col)
                    }
                    Spacer(Modifier.height(4.dp))
                    T(c.text, 12.sp, C.DIM)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CalculosTab(m: Manager) {
    Live { _ ->
        val v = m.values
        val cfg = m.cfg
        val lh = Calc.fuelLh(v, cfg)
        val af = Calc.airFuel(v, cfg)
        val cv = Calc.powerCv(v, cfg)
        val nm = Calc.torqueNm(v, cfg)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Panel {
                KeyValue("Consumo (L/h)", Fmt.num(lh, 2))
                KeyValue("Consumo (km/l)", Fmt.num(m.instKmL, 1))
                KeyValue("Método", Calc.method(v))
                KeyValue("Combustível", cfg.fuelName() + (v[0x52]?.let { " (ECU informa ${Fmt.num(it, 0)}% etanol)" } ?: ""))
                KeyValue("Lambda comandado", Fmt.num(v[0x44], 3))
                KeyValue("Relação ar/combustível", if (af == null) "—" else Fmt.num(af, 2) + " : 1")
                KeyValue("Potência estimada", if (cv == null) "—" else Fmt.num(cv, 0) + " cv")
                KeyValue("Torque estimado", if (nm == null) "—" else Fmt.num(nm, 0) + " N·m")
            }
            T("Potência e torque saem do combustível queimado (rendimento de 28%). É só ordem de grandeza, não substitui um dinamômetro.", 11.sp, C.DIM)
            Panel {
                Label("Da viagem")
                Spacer(Modifier.height(6.dp))
                KeyValue("Total de km (app)", Fmt.num(cfg.odo.toDouble(), 0))
                KeyValue("Média desde a instalação", if (cfg.lifeLiters > 1f) Fmt.num((cfg.lifeDist / cfg.lifeLiters).toDouble(), 1) + " km/l" else "—")
                KeyValue("Latência do ciclo", "${m.avgLatency} ms")
                KeyValue("Leitura em lote", if (m.batchOk) "ativa" else "inativa")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProntidaoTab(m: Manager) {
    var res by remember { mutableStateOf<IntArray?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Mostra se a ECU já terminou os autotestes (monitores). Útil para saber se o carro passaria numa inspeção de emissões.", 12.sp, C.DIM)
        PrimaryButton(if (busy) "Lendo…" else "Ler prontidão", enabled = !busy) {
            busy = true; err = ""
            m.readReadiness { r ->
                busy = false
                r.onSuccess { res = it; if (it == null) err = "A ECU não respondeu." }
                r.onFailure { err = it.message ?: "Erro" }
            }
        }
        if (err.isNotEmpty()) T(err, 13.sp, C.BAD)
        val b = res
        if (b != null && b.size >= 4) {
            val mil = (b[0] and 0x80) != 0
            Panel {
                KeyValue("Luz de injeção (MIL)", if (mil) "Acesa" else "Apagada", if (mil) C.BAD else C.GOOD)
                KeyValue("Falhas registradas", "${b[0] and 0x7F}")
            }
            val spark = (b[1] and 0x08) != 0
            Panel {
                Label(if (spark) "Motor a diesel" else "Motor a gasolina/etanol")
                Spacer(Modifier.height(6.dp))
                fun row(name: String, bitSupported: Int, bitIncomplete: Int, byteIdx: Int) {
                }
                val items = ArrayList<Triple<String, Boolean, Boolean>>() // nome, suportado, completo
                fun add(name: String, idx: Int, sBit: Int, iBit: Int, offset: Int) {
                    val sup = ((b[idx] shr sBit) and 1) == 1
                    val inc = ((b[idx + 1] shr iBit) and 1) == 1
                    items.add(Triple(name, sup, !inc))
                }
                // contínuos (byte 1: bits 0-2 suportados, 4-6 incompletos)
                val cont = listOf("Falhas de ignição" to 0, "Sistema de combustível" to 1, "Componentes" to 2)
                for ((n, bit) in cont) {
                    val sup = ((b[1] shr bit) and 1) == 1
                    val inc = ((b[1] shr (bit + 4)) and 1) == 1
                    items.add(Triple(n, sup, !inc))
                }
                val names = if (spark) listOf(
                    "Catalisador", "Catalisador aquecido", "Sistema EVAP", "Ar secundário",
                    "Sistema de A/C", "Sonda lambda", "Aquecedor da sonda", "Recirculação (EGR)"
                ) else listOf(
                    "Catalisador NMHC", "Pós-tratamento NOx", "—", "Sensor de pressão do turbo",
                    "—", "Sensor de gases", "Filtro de partículas", "Recirculação (EGR)"
                )
                for (i in 0..7) {
                    if (names[i] == "—") continue
                    val sup = ((b[2] shr i) and 1) == 1
                    val inc = ((b[3] shr i) and 1) == 1
                    items.add(Triple(names[i], sup, !inc))
                }
                for ((n, sup, ok) in items) {
                    if (!sup) continue
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        T(n, 13.sp, C.TEXT, modifier = Modifier.weight(1f))
                        Pill(if (ok) "pronto" else "incompleto", if (ok) C.GOOD else C.WARN)
                    }
                }
                if (items.none { it.second }) T("Nenhum monitor informado.", 12.sp, C.DIM)
            }
            T("“Incompleto” é normal logo depois de apagar falhas ou trocar a bateria: o carro precisa rodar alguns ciclos.", 11.sp, C.DIM)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Modo06Tab(m: Manager) {
    var res by remember { mutableStateOf<List<Obd.Mon>?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Resultados dos testes que a própria ECU faz (catalisador, sondas, falhas de ignição). Mostra o valor medido e os limites; fora da faixa indica o componente que está no limite.", 12.sp, C.DIM)
        PrimaryButton(if (busy) "Lendo…" else "Ler Modo 06", enabled = !busy) {
            busy = true; err = ""
            m.readMode06 { r ->
                busy = false
                r.onSuccess { res = it; if (it.isEmpty()) err = "Esta ECU não informa testes no Modo 06 (comum em alguns modelos)." }
                r.onFailure { err = it.message ?: "Erro" }
            }
        }
        if (err.isNotEmpty()) T(err, 13.sp, C.WARN)
        val list = res
        if (list != null) {
            for (mon in list) {
                val col = if (mon.ok) C.GOOD else C.BAD
                Panel(pad = 10.dp, border = col.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(Obd.monName(mon.mid), 13.sp, C.TEXT, FontWeight.Medium, Modifier.weight(1f))
                        Pill(if (mon.ok) "dentro" else "fora", col)
                    }
                    T("teste %02X · valor ${mon.value} · limites ${mon.min} a ${mon.max}".format(mon.tid), 11.sp, C.DIM)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CongeladosTab(m: Manager) {
    var res by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Foto dos sensores no instante em que a última falha foi registrada: mostra em que condição o problema aconteceu.", 12.sp, C.DIM)
        PrimaryButton(if (busy) "Lendo…" else "Ler dados congelados", enabled = !busy) {
            busy = true; err = ""
            m.readFreeze { r ->
                busy = false
                r.onSuccess { res = it }
                r.onFailure { err = it.message ?: "Erro" }
            }
        }
        if (err.isNotEmpty()) T(err, 13.sp, C.BAD)
        res?.let { l -> Panel { for ((k, v) in l) KeyValue(k, v) } }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ModulosTab(m: Manager) {
    var res by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Procura outros módulos do carro que respondam pelo padrão aberto (somente leitura). Módulos de fábrica da Fiat — ABS, airbag, painel — costumam exigir o protocolo próprio da marca e podem não aparecer.", 12.sp, C.DIM)
        PrimaryButton(if (busy) "Varrendo… (até 40 s)" else "Varrer módulos", enabled = !busy) {
            busy = true; err = ""
            m.scanModules { r ->
                busy = false
                r.onSuccess { res = it }
                r.onFailure { err = it.message ?: "Erro" }
            }
        }
        if (err.isNotEmpty()) T(err, 13.sp, C.BAD)
        res?.let { l -> Panel { for ((k, v) in l) KeyValue(k, v) } }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LeitorTab(m: Manager) {
    var res by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Mede a velocidade e a estabilidade do seu leitor ELM327. Clones baratos são lentos e às vezes perdem respostas.", 12.sp, C.DIM)
        PrimaryButton(if (busy) "Testando…" else "Testar o leitor", enabled = !busy) {
            busy = true; err = ""
            m.adapterTest { r ->
                busy = false
                r.onSuccess { res = it }
                r.onFailure { err = it.message ?: "Erro" }
            }
        }
        if (err.isNotEmpty()) T(err, 13.sp, C.BAD)
        res?.let { l -> Panel { for ((k, v) in l) KeyValue(k, v) } }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TerminalTab(m: Manager) {
    var cmd by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun send(c: String) {
        if (c.isBlank() || busy) return
        busy = true
        m.terminal(c.trim()) { r ->
            busy = false
            val out = r.getOrNull() ?: ("erro: " + (r.exceptionOrNull()?.message ?: ""))
            log = "> ${c.trim()}\n$out\n\n$log"
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        T("Envie comandos direto ao leitor (AT…) ou à ECU (hex). Ex.: 010C, 0902, 22F190 (Modo 22 / UDS), ATDP.", 12.sp, C.DIM)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cmd, onValueChange = { cmd = it.uppercase() }, singleLine = true,
                modifier = Modifier.weight(1f),
                placeholder = { T("comando", 13.sp, C.DIM) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Send),
                colors = fieldColors()
            )
            PrimaryButton(if (busy) "…" else "Enviar", enabled = !busy) { send(cmd); cmd = "" }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in listOf("ATI", "ATDP", "ATRV", "0100", "0120", "0902", "0904", "03", "0101", "22F190")) Chip(c, false) { send(c) }
        }
        Panel(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                T(log.ifEmpty { "Nenhum comando enviado ainda." }, 12.sp, if (log.isEmpty()) C.DIM else C.TEAL, mono = true)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun InfoTab(m: Manager) {
    var res by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Identificação do carro e da ECU (chassi, calibração) e dos sensores que a ECU aceita.", 12.sp, C.DIM)
        PrimaryButton(if (busy) "Lendo…" else "Ler informações", enabled = !busy) {
            busy = true; err = ""
            m.readInfo { r ->
                busy = false
                r.onSuccess { res = it }
                r.onFailure { err = it.message ?: "Erro" }
            }
        }
        if (err.isNotEmpty()) T(err, 13.sp, C.BAD)
        res?.let { l -> Panel { for ((k, v) in l) KeyValue(k, v) } }
        Spacer(Modifier.height(12.dp))
    }
}
