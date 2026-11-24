package br.com.edideias.argoobd

import java.util.Locale

class PidDef(
    val pid: Int,
    val name: String,
    val unit: String,
    val len: Int,
    val dec: Int,
    val f: (IntArray) -> Double
)

object Pids {
    const val VOLT = 0x1000 // tensão lida pelo leitor (ATRV)

    private fun u16(d: IntArray): Int = d[0] * 256 + d[1]

    val list: List<PidDef> = buildList {
        add(PidDef(0x04, "Carga do motor", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x05, "Temperatura da água", "°C", 1, 0) { it[0] - 40.0 })
        add(PidDef(0x06, "Correção comb. curta B1", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 })
        add(PidDef(0x07, "Correção comb. longa B1", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 })
        add(PidDef(0x08, "Correção comb. curta B2", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 })
        add(PidDef(0x09, "Correção comb. longa B2", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 })
        add(PidDef(0x0A, "Pressão do combustível", "kPa", 1, 0) { it[0] * 3.0 })
        add(PidDef(0x0B, "Pressão do coletor (MAP)", "kPa", 1, 0) { it[0].toDouble() })
        add(PidDef(0x0C, "Rotação", "rpm", 2, 0) { u16(it) / 4.0 })
        add(PidDef(0x0D, "Velocidade", "km/h", 1, 0) { it[0].toDouble() })
        add(PidDef(0x0E, "Avanço de ignição", "°", 1, 1) { it[0] / 2.0 - 64 })
        add(PidDef(0x0F, "Temperatura do ar admitido", "°C", 1, 0) { it[0] - 40.0 })
        add(PidDef(0x10, "Fluxo de ar (MAF)", "g/s", 2, 2) { u16(it) / 100.0 })
        add(PidDef(0x11, "Posição do acelerador", "%", 1, 0) { it[0] * 100.0 / 255 })
        val o2 = listOf("B1S1", "B1S2", "B1S3", "B1S4", "B2S1", "B2S2", "B2S3", "B2S4")
        for (i in 0..7) {
            add(PidDef(0x14 + i, "Sonda O2 ${o2[i]} (tensão)", "V", 2, 3) { it[0] / 200.0 })
        }
        add(PidDef(0x1F, "Tempo com motor ligado", "s", 2, 0) { u16(it).toDouble() })
        add(PidDef(0x21, "Distância com luz de falha acesa", "km", 2, 0) { u16(it).toDouble() })
        add(PidDef(0x22, "Pressão do trilho (relativa)", "kPa", 2, 1) { u16(it) * 0.079 })
        add(PidDef(0x23, "Pressão do trilho (direta)", "kPa", 2, 0) { u16(it) * 10.0 })
        for (i in 0..7) {
            add(PidDef(0x24 + i, "Sonda larga ${o2[i]} (lambda)", "λ", 2, 3) { u16(it) * 2.0 / 65536 })
        }
        add(PidDef(0x2C, "EGR comandado", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x2D, "Erro do EGR", "%", 1, 1) { (it[0] - 128) * 100.0 / 128 })
        add(PidDef(0x2E, "Purga do canister", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x2F, "Nível de combustível", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x30, "Aquecimentos desde limpeza", "", 1, 0) { it[0].toDouble() })
        add(PidDef(0x31, "Distância desde limpeza de falhas", "km", 2, 0) { u16(it).toDouble() })
        add(PidDef(0x32, "Pressão de vapor EVAP", "Pa", 2, 0) { u16(it).toShort() / 4.0 })
        add(PidDef(0x33, "Pressão barométrica", "kPa", 1, 0) { it[0].toDouble() })
        add(PidDef(0x3C, "Temp. catalisador B1S1", "°C", 2, 0) { u16(it) / 10.0 - 40 })
        add(PidDef(0x3D, "Temp. catalisador B2S1", "°C", 2, 0) { u16(it) / 10.0 - 40 })
        add(PidDef(0x3E, "Temp. catalisador B1S2", "°C", 2, 0) { u16(it) / 10.0 - 40 })
        add(PidDef(0x3F, "Temp. catalisador B2S2", "°C", 2, 0) { u16(it) / 10.0 - 40 })
        add(PidDef(0x42, "Tensão do módulo (ECU)", "V", 2, 2) { u16(it) / 1000.0 })
        add(PidDef(0x43, "Carga absoluta", "%", 2, 0) { u16(it) * 100.0 / 255 })
        add(PidDef(0x44, "Lambda comandado", "λ", 2, 3) { u16(it) / 32768.0 })
        add(PidDef(0x45, "Acelerador relativo", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x46, "Temperatura ambiente", "°C", 1, 0) { it[0] - 40.0 })
        add(PidDef(0x47, "Acelerador absoluto B", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x49, "Pedal do acelerador D", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x4A, "Pedal do acelerador E", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x4B, "Pedal do acelerador F", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x4C, "Acelerador comandado", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x4D, "Tempo com luz de falha acesa", "min", 2, 0) { u16(it).toDouble() })
        add(PidDef(0x4E, "Tempo desde limpeza de falhas", "min", 2, 0) { u16(it).toDouble() })
        add(PidDef(0x52, "Etanol no combustível", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x5A, "Pedal relativo", "%", 1, 0) { it[0] * 100.0 / 255 })
        add(PidDef(0x5C, "Temperatura do óleo", "°C", 1, 0) { it[0] - 40.0 })
        add(PidDef(0x5D, "Avanço da injeção", "°", 2, 1) { u16(it) / 128.0 - 210 })
        add(PidDef(0x5E, "Consumo (taxa de combustível)", "L/h", 2, 2) { u16(it) / 20.0 })
    }

    val map: Map<Int, PidDef> = list.associateBy { it.pid }

    fun nameOf(pid: Int): String =
        if (pid == VOLT) "Tensão da bateria (leitor)" else map[pid]?.name ?: "PID %02X".format(pid)

    fun unitOf(pid: Int): String =
        if (pid == VOLT) "V" else map[pid]?.unit ?: ""

    fun decOf(pid: Int): Int = if (pid == VOLT) 1 else map[pid]?.dec ?: 0
}

object Fmt {
    private val loc = Locale("pt", "BR")
    fun num(v: Double?, dec: Int): String {
        if (v == null || v.isNaN() || v.isInfinite()) return "—"
        return String.format(loc, "%.${dec}f", v)
    }

    fun pid(pid: Int, v: Double?): String = num(v, Pids.decOf(pid))

    fun time(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return if (h > 0) String.format(loc, "%dh %02dmin", h, m) else String.format(loc, "%d:%02d", m, s)
    }

    fun money(v: Double?): String = if (v == null) "—" else String.format(loc, "R$ %.2f", v)
}

object Obd {
    /** Quebra a resposta do ELM em quadros de bytes (junta as linhas "0:", "1:" de respostas longas). */
    fun frames(resp: String): List<IntArray> {
        val out = ArrayList<IntArray>()
        val multi = ArrayList<Int>()
        fun flush() {
            if (multi.isNotEmpty()) {
                out.add(multi.toIntArray())
                multi.clear()
            }
        }
        for (raw in resp.split('\r', '\n')) {
            var line = raw.trim().uppercase().replace(" ", "")
            if (line.isEmpty()) continue
            var marker: Char? = null
            if (line.length >= 2 && line[1] == ':') {
                marker = line[0]
                line = line.substring(2)
            }
            if (!line.all { it in '0'..'9' || it in 'A'..'F' }) continue
            if (line.isEmpty() || line.length % 2 != 0) continue
            val bytes = IntArray(line.length / 2) { line.substring(it * 2, it * 2 + 2).toInt(16) }
            if (marker != null) {
                if (marker == '0') flush()
                for (b in bytes) multi.add(b)
            } else {
                flush()
                out.add(bytes)
            }
        }
        flush()
        return out
    }

    /** Bytes depois do cabeçalho "4x pid". */
    fun data(resp: String, mode: Int, pid: Int?): IntArray? {
        for (f in frames(resp)) {
            if (f.isEmpty() || f[0] != mode + 0x40) continue
            if (pid == null) return f.copyOfRange(1, f.size)
            if (f.size >= 2 && f[1] == pid) return f.copyOfRange(2, f.size)
        }
        return null
    }

    fun value(resp: String, d: PidDef): Double? {
        val b = data(resp, 1, d.pid) ?: return null
        if (b.size < d.len) return null
        return d.f(b)
    }

    fun supportMask(resp: String, base: Int, mode: Int = 1): Set<Int>? {
        val b = data(resp, mode, base) ?: return null
        if (b.size < 4) return null
        val out = HashSet<Int>()
        for (i in 0 until 32) {
            if (((b[i / 8] shr (7 - i % 8)) and 1) == 1) out.add(base + i + 1)
        }
        return out
    }

    fun code(a: Int, b: Int): String {
        val sys = "PCBU"[(a shr 6) and 3]
        val d1 = (a shr 4) and 3
        return "%c%d%X%02X".format(sys, d1, a and 0xF, b)
    }

    /** Retorna null se a ECU não respondeu ao modo; lista vazia se não há falhas. */
    fun dtcs(resp: String, mode: Int): List<String>? {
        val out = LinkedHashSet<String>()
        var found = false
        for (f in frames(resp)) {
            if (f.isEmpty() || f[0] != mode + 0x40) continue
            found = true
            var rest = f.copyOfRange(1, f.size)
            if (rest.size % 2 == 1) rest = rest.copyOfRange(1, rest.size)
            var i = 0
            while (i + 1 < rest.size) {
                val a = rest[i]
                val b = rest[i + 1]
                i += 2
                if (a == 0 && b == 0) continue
                out.add(code(a, b))
            }
        }
        return if (found) out.toList() else null
    }

    fun ascii(resp: String, pid: Int, skip: Int = 1): String? {
        for (f in frames(resp)) {
            if (f.size > 2 + skip && f[0] == 0x49 && f[1] == pid) {
                val s = f.drop(2 + skip)
                    .filter { it in 0x20..0x7E }
                    .map { it.toChar() }
                    .joinToString("")
                    .trim()
                if (s.isNotEmpty()) return s
            }
        }
        return null
    }

    fun vin(resp: String): String? {
        for (f in frames(resp)) {
            if (f.size > 3 && f[0] == 0x49 && f[1] == 0x02) {
                val s = f.drop(3)
                    .filter { (it in 0x30..0x39) || (it in 0x41..0x5A) }
                    .map { it.toChar() }
                    .joinToString("")
                if (s.length >= 10) return s.take(17)
            }
        }
        return null
    }

    /** Dados congelados: devolve os bytes depois de "42 pid frame". */
    fun freeze(resp: String, pid: Int): IntArray? {
        for (f in frames(resp)) {
            if (f.size >= 3 && f[0] == 0x42 && f[1] == pid) return f.copyOfRange(3, f.size)
        }
        return null
    }

    /** Resposta a vários PIDs numa só consulta (ex.: 010C0D05). */
    fun multi(resp: String, pids: List<Int>): Map<Int, Double>? {
        for (f in frames(resp)) {
            if (f.isEmpty() || f[0] != 0x41) continue
            var i = 1
            val out = HashMap<Int, Double>()
            while (i < f.size) {
                val pid = f[i]
                val d = Pids.map[pid] ?: break
                if (i + 1 + d.len > f.size) break
                out[pid] = d.f(f.copyOfRange(i + 1, i + 1 + d.len))
                i += 1 + d.len
            }
            val r = out.filterKeys { it in pids }
            if (r.isNotEmpty()) return r
        }
        return null
    }

    class Mon(val mid: Int, val tid: Int, val uas: Int, val value: Int, val min: Int, val max: Int) {
        val ok: Boolean get() = value in min..max
    }

    /** Modo 06: resultados dos testes de monitoramento da ECU. */
    fun mode06(resp: String): List<Mon> {
        val out = ArrayList<Mon>()
        for (f in frames(resp)) {
            if (f.size < 10 || f[0] != 0x46) continue
            val mid = f[1]
            var i = 2
            while (i + 8 <= f.size) {
                val tid = f[i]
                val uas = f[i + 1]
                val v = f[i + 2] * 256 + f[i + 3]
                val mn = f[i + 4] * 256 + f[i + 5]
                val mx = f[i + 6] * 256 + f[i + 7]
                i += 8
                if (tid == 0 && v == 0 && mn == 0 && mx == 0) continue
                out.add(Mon(mid, tid, uas, v, mn, mx))
            }
        }
        return out
    }

    fun monName(mid: Int): String = when (mid) {
        in 0x01..0x04 -> "Sonda lambda B1S${mid}"
        in 0x05..0x08 -> "Sonda lambda B2S${mid - 4}"
        0x21 -> "Catalisador banco 1"
        0x22 -> "Catalisador banco 2"
        0x31 -> "EGR banco 1"
        0x32 -> "EGR banco 2"
        0x39 -> "EVAP (0,090\")"
        0x3A -> "EVAP (0,040\")"
        0x3B -> "EVAP (0,020\")"
        0x3C -> "EVAP (válvula de purga)"
        in 0x41..0x44 -> "Aquecedor sonda B1S${mid - 0x40}"
        in 0x45..0x48 -> "Aquecedor sonda B2S${mid - 0x44}"
        0x71 -> "Ar secundário"
        0xA1 -> "Falhas de ignição (geral)"
        0xA2 -> "Falhas de ignição cilindro 1"
        0xA3 -> "Falhas de ignição cilindro 2"
        0xA4 -> "Falhas de ignição cilindro 3"
        0xA5 -> "Falhas de ignição cilindro 4"
        else -> "Monitor %02X".format(mid)
    }

    /** Leitura UDS (serviço 22): bytes depois de "62 hh ll". */
    fun did(resp: String, did: Int): IntArray? {
        for (f in frames(resp)) {
            if (f.size >= 3 && f[0] == 0x62 && f[1] == ((did shr 8) and 0xFF) && f[2] == (did and 0xFF)) {
                return f.copyOfRange(3, f.size)
            }
        }
        return null
    }

    fun asciiOf(b: IntArray): String =
        b.filter { it in 0x20..0x7E }.map { it.toChar() }.joinToString("").trim()

    /** Endereços de ECU que responderam (com cabeçalhos ligados, ATH1 e ATS0). */
    fun ecuIds(resp: String): List<String> {
        val out = LinkedHashSet<String>()
        for (raw in resp.split('\r', '\n')) {
            val line = raw.trim().uppercase().replace(" ", "")
            if (line.length >= 6 && line.take(3).all { it in '0'..'9' || it in 'A'..'F' } &&
                line.take(3).startsWith("7") && line.all { it in '0'..'9' || it in 'A'..'F' }
            ) out.add(line.take(3))
        }
        return out.toList()
    }

    fun cleanLine(resp: String): String =
        resp.split('\r', '\n').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
}
