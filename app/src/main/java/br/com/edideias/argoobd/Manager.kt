package br.com.edideias.argoobd

import android.bluetooth.BluetoothManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class DtcResult(
    val stored: List<String>?,
    val pending: List<String>?,
    val permanent: List<String>?
)

class Manager(private val ctx: Context, val cfg: Cfg) {
    private val main = Handler(Looper.getMainLooper())

    val values = ConcurrentHashMap<Int, Double>()
    val stamps = ConcurrentHashMap<Int, Long>()
    val mins = ConcurrentHashMap<Int, Double>()
    val maxs = ConcurrentHashMap<Int, Double>()
    private val hist = ConcurrentHashMap<Int, ArrayList<Float>>()
    private val miss = HashMap<Int, Int>()

    @Volatile var supported: List<Int> = emptyList()
    @Volatile var status: String = "Desconectado"
    @Volatile var connected = false
    @Volatile var ecuOk = false
    @Volatile var demoMode = false
    @Volatile var protocol = ""
    @Volatile var elm = ""
    @Volatile var milOn = false
    @Volatile var dtcCount = 0
    @Volatile var liveMode = 0 // 0 = painel, 1 = oficina
    @Volatile var instKmL: Double? = null
    @Volatile var instLh: Double? = null

    val trip = Trip(cfg.sp)

    private var thread: Thread? = null
    @Volatile private var want = false
    private val tasks = LinkedBlockingQueue<(Link) -> Unit>()
    private var lastTripT = 0L

    // -------- controle --------

    @Synchronized
    fun start(addr: String?, demo: Boolean) {
        stop()
        want = true
        demoMode = demo
        status = "Conectando…"
        thread = Thread { runLoop(addr, demo) }.apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        want = false
        thread?.interrupt()
        thread = null
        connected = false
        ecuOk = false
        status = "Desconectado"
        tasks.clear()
        stopLogQuiet()
        trip.save()
    }

    fun isRunning() = want

    private fun runLoop(addr: String?, demo: Boolean) {
        while (want) {
            var link: Link? = null
            try {
                status = "Conectando ao leitor…"
                link = if (demo) DemoLink() else makeBt(addr)
                link.open()
                connected = true
                initElm(link)
                session(link)
            } catch (e: InterruptedException) {
                break
            } catch (e: SecurityException) {
                status = "Permita o Bluetooth para o app"
                want = false
            } catch (e: Exception) {
                if (want) status = "Sem conexão com o leitor — tentando de novo…"
            } finally {
                connected = false
                ecuOk = false
                try { link?.close() } catch (_: Exception) {}
            }
            if (want) {
                try { Thread.sleep(3000) } catch (e: InterruptedException) { break }
            }
        }
        if (!want) {
            connected = false
            ecuOk = false
            if (!status.startsWith("Permita")) status = "Desconectado"
        }
    }

    @Suppress("MissingPermission")
    private fun makeBt(addr: String?): Link {
        if (addr.isNullOrEmpty()) throw IOException("sem leitor escolhido")
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val ad = bm.adapter ?: throw IOException("sem bluetooth")
        if (!ad.isEnabled) throw IOException("bluetooth desligado")
        try { ad.cancelDiscovery() } catch (_: Exception) {}
        return BtLink(ad.getRemoteDevice(addr))
    }

    private fun initElm(link: Link) {
        val z = link.query("ATZ", 6000)
        val line = Obd.cleanLine(z)
        elm = if (line.uppercase().contains("ELM")) line.substring(line.uppercase().indexOf("ELM")) else line
        for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATAT1")) link.query(c, 2500)
        link.query("ATSP0", 2500)
    }

    private fun session(link: Link) {
        miss.clear()
        status = "Leitor conectado — procurando a ECU…"
        var mask0: Set<Int>? = null
        var tries = 0
        while (want && mask0 == null) {
            val r = link.query("0100", 15000)
            mask0 = Obd.supportMask(r, 0)
            if (mask0 == null) {
                tries++
                status = "Leitor conectado — ligue a ignição do carro"
                runTasks(link)
                Thread.sleep(2000)
                if (tries > 40) throw IOException("ECU não respondeu")
            }
        }
        if (mask0 == null) return
        val sup = TreeSet<Int>()
        sup.addAll(mask0)
        var base = 0x20
        var more = mask0.contains(0x20)
        while (more && base <= 0xC0 && want) {
            val m = Obd.supportMask(link.query("01%02X".format(base), 4000), base)
            if (m == null) break
            sup.addAll(m)
            more = m.contains(base + 0x20)
            base += 0x20
        }
        sup.removeAll(setOf(0x00, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0))
        var list = sup.filter { Pids.map.containsKey(it) }
        if (list.isEmpty()) list = listOf(0x0C, 0x0D, 0x05, 0x04, 0x11, 0x0B, 0x0F, 0x10)
        supported = list
        val dp = Obd.cleanLine(link.query("ATDP", 2500))
        protocol = dp.removePrefix("AUTO, ").removePrefix("AUTO,")
        ecuOk = true
        status = "Conectado"
        readStatus(link)
        poll(link)
    }

    private fun tier1(sup: List<Int>): List<Int> {
        val out = ArrayList<Int>()
        if (sup.contains(0x0C)) out.add(0x0C)
        if (sup.contains(0x0D)) out.add(0x0D)
        when {
            sup.contains(0x5E) -> out.add(0x5E)
            sup.contains(0x10) -> out.add(0x10)
            sup.contains(0x0B) -> out.add(0x0B)
        }
        return out
    }

    private val painelSlow = listOf(0x05, 0x04, 0x11, 0x0F, 0x0B, 0x10, 0x52, 0x2F, 0x0E, 0x5C, 0x46)

    private fun poll(link: Link) {
        var slowIdx = 0
        var lastV = 0L
        var lastMil = System.currentTimeMillis()
        var fails = 0
        lastTripT = System.currentTimeMillis()
        while (want) {
            runTasks(link)
            val sup = supported
            val t1 = tier1(sup)
            var ok = 0
            for (pid in t1) if (readPid(link, pid)) ok++
            val others: List<Int> = if (liveMode == 1) {
                sup.filter { it !in t1 }
            } else {
                painelSlow.filter { it in sup && it !in t1 }
            }
            val k = if (liveMode == 1) 3 else 2
            repeat(k) {
                if (others.isNotEmpty()) {
                    val pid = others[slowIdx % others.size]
                    slowIdx++
                    if (readPid(link, pid)) ok++
                }
            }
            val now = System.currentTimeMillis()
            if (now - lastV > 4000) {
                readVolt(link)
                lastV = now
            }
            if (now - lastMil > 25000) {
                readStatus(link)
                lastMil = now
            }
            if (ok == 0) {
                fails++
                if (fails >= 4) {
                    ecuOk = false
                    status = "ECU sem resposta — motor/ignição ligados?"
                }
                if (fails >= 10) throw IOException("ECU parou de responder")
            } else {
                fails = 0
                ecuOk = true
                status = "Conectado"
            }
            updateTrip()
            logRow()
        }
    }

    private fun readPid(link: Link, pid: Int): Boolean {
        val d = Pids.map[pid] ?: return false
        val r = link.query("01%02X".format(pid), 1800)
        val v = Obd.value(r, d)
        if (v == null) {
            val n = (miss[pid] ?: 0) + 1
            miss[pid] = n
            if (n >= 6 && pid != 0x0C && pid != 0x0D) {
                supported = supported - pid
                miss.remove(pid)
            }
            return false
        }
        miss[pid] = 0
        put(pid, v)
        return true
    }

    private fun readVolt(link: Link) {
        val r = link.query("ATRV", 1500)
        val m = Regex("([0-9]+[.,][0-9]+)").find(r) ?: return
        val v = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return
        put(Pids.VOLT, v)
    }

    private fun readStatus(link: Link) {
        val r = link.query("0101", 3000)
        val b = Obd.data(r, 1, 0x01) ?: return
        if (b.size >= 4) {
            milOn = (b[0] and 0x80) != 0
            dtcCount = b[0] and 0x7F
        }
    }

    private fun put(pid: Int, v: Double) {
        values[pid] = v
        stamps[pid] = System.currentTimeMillis()
        val mn = mins[pid]
        if (mn == null || v < mn) mins[pid] = v
        val mx = maxs[pid]
        if (mx == null || v > mx) maxs[pid] = v
        val h = hist.getOrPut(pid) { ArrayList() }
        synchronized(h) {
            h.add(v.toFloat())
            if (h.size > 240) h.removeAt(0)
        }
    }

    fun history(pid: Int): FloatArray {
        val h = hist[pid] ?: return FloatArray(0)
        synchronized(h) { return h.toFloatArray() }
    }

    fun resetMinMax() {
        mins.clear()
        maxs.clear()
    }

    /** valor recente (até 5 s) ou null */
    fun fresh(pid: Int, ms: Long = 5000): Double? {
        val s = stamps[pid] ?: return null
        if (System.currentTimeMillis() - s > ms) return null
        return values[pid]
    }

    private fun updateTrip() {
        val now = System.currentTimeMillis()
        val dt = (now - lastTripT) / 1000.0
        lastTripT = now
        if (dt <= 0 || dt > 3.5) return
        val rpm = fresh(0x0C)
        if (rpm == null || rpm < 300) {
            instKmL = null
            instLh = null
            return
        }
        val spd = fresh(0x0D) ?: 0.0
        val lh = Calc.fuelLh(values, cfg)
        instLh = lh
        instKmL = if (lh != null && lh > 0.05 && spd > 3) spd / lh else null
        trip.add(dt, spd, lh ?: 0.0, cfg.price())
    }

    // -------- tarefas pedidas pela tela --------

    private fun runTasks(link: Link) {
        while (true) {
            val t = tasks.poll() ?: break
            t(link)
        }
    }

    fun <T> ask(block: (Link) -> T, cb: (Result<T>) -> Unit) {
        if (!connected || !want) {
            cb(Result.failure(IOException("Sem conexão com o leitor")))
            return
        }
        tasks.add { l ->
            val r = runCatching { block(l) }
            main.post { cb(r) }
        }
    }

    fun readDtcs(cb: (Result<DtcResult>) -> Unit) = ask({ l ->
        val s = Obd.dtcs(l.query("03", 5000), 0x03)
        val p = Obd.dtcs(l.query("07", 5000), 0x07)
        val m = Obd.dtcs(l.query("0A", 5000), 0x0A)
        DtcResult(s, p, m)
    }, cb)

    fun clearDtcs(cb: (Result<Boolean>) -> Unit) = ask({ l ->
        val r = l.query("04", 8000)
        val ok = Obd.frames(r).any { it.isNotEmpty() && it[0] == 0x44 }
        if (ok) {
            milOn = false
            dtcCount = 0
        }
        ok
    }, cb)

    fun readReadiness(cb: (Result<IntArray?>) -> Unit) = ask({ l ->
        Obd.data(l.query("0101", 4000), 1, 0x01)
    }, cb)

    fun readFreeze(cb: (Result<List<Pair<String, String>>>) -> Unit) = ask({ l ->
        val out = ArrayList<Pair<String, String>>()
        val dtc = Obd.freeze(l.query("020200", 4000), 0x02)
        if (dtc != null && dtc.size >= 2 && (dtc[0] != 0 || dtc[1] != 0)) {
            val code = Obd.code(dtc[0], dtc[1])
            out.add("Falha que gerou o registro" to "$code — ${Dtc.describe(code)}")
        } else {
            out.add("Falha que gerou o registro" to "nenhuma")
        }
        for (pid in listOf(0x04, 0x05, 0x06, 0x07, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11)) {
            val d = Pids.map[pid] ?: continue
            val b = Obd.freeze(l.query("02%02X00".format(pid), 3000), pid) ?: continue
            if (b.size < d.len) continue
            out.add(d.name to (Fmt.num(d.f(b), d.dec) + " " + d.unit))
        }
        out.toList()
    }, cb)

    fun readInfo(cb: (Result<List<Pair<String, String>>>) -> Unit) = ask({ l ->
        val out = ArrayList<Pair<String, String>>()
        out.add("Leitor" to elm.ifEmpty { "—" })
        out.add("Protocolo" to protocol.ifEmpty { "—" })
        val vin = Obd.vin(l.query("0902", 6000))
        out.add("Chassi (VIN)" to (vin ?: "a ECU não informou"))
        out.add("ID de calibração" to (Obd.ascii(l.query("0904", 6000), 0x04) ?: "a ECU não informou"))
        out.add("Nome da ECU" to (Obd.ascii(l.query("090A", 6000), 0x0A) ?: "a ECU não informou"))
        out.add("Tensão no conector" to (values[Pids.VOLT]?.let { Fmt.num(it, 1) + " V" } ?: "—"))
        out.add(
            "PIDs aceitos (${supported.size})" to
                supported.joinToString(" ") { "%02X".format(it) }.ifEmpty { "—" }
        )
        out.toList()
    }, cb)

    fun terminal(cmd: String, cb: (Result<String>) -> Unit) = ask({ l ->
        l.query(cmd, 10000).trim()
    }, cb)

    // -------- gravação CSV --------

    @Volatile var logging = false
    private var lw: BufferedWriter? = null
    private var logCols: List<Int> = emptyList()
    private var logT0 = 0L
    private var logRows = 0
    private val logFile: File get() = File(ctx.cacheDir, "log.csv")

    @Synchronized
    fun startLog(): Boolean {
        if (logging) return true
        return try {
            logCols = (supported + Pids.VOLT).distinct()
            val w = logFile.bufferedWriter()
            w.write("tempo_s;" + logCols.joinToString(";") { Pids.nameOf(it) + " (" + Pids.unitOf(it) + ")" })
            w.newLine()
            lw = w
            logT0 = System.currentTimeMillis()
            logRows = 0
            logging = true
            true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    private fun logRow() {
        if (!logging) return
        val w = lw ?: return
        try {
            val t = (System.currentTimeMillis() - logT0) / 1000.0
            val sb = StringBuilder(Fmt.num(t, 1))
            for (p in logCols) {
                sb.append(';')
                val v = values[p]
                if (v != null) sb.append(Fmt.num(v, 3))
            }
            w.write(sb.toString())
            w.newLine()
            logRows++
        } catch (e: Exception) {
            logging = false
        }
    }

    @Synchronized
    private fun stopLogQuiet() {
        if (!logging) return
        logging = false
        try { lw?.close() } catch (_: Exception) {}
        lw = null
    }

    /** Fecha a gravação e salva em Downloads. Devolve a mensagem para mostrar. */
    @Synchronized
    fun stopLog(): String {
        if (!logging) return "Não havia gravação em andamento"
        logging = false
        try { lw?.close() } catch (_: Exception) {}
        lw = null
        if (logRows == 0) return "Gravação vazia — nada salvo"
        val name = "ArgoOBD-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".csv"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    ?: return "Não consegui salvar o arquivo"
                ctx.contentResolver.openOutputStream(uri)!!.use { o ->
                    logFile.inputStream().use { it.copyTo(o) }
                }
                "Salvo em Downloads/$name ($logRows linhas)"
            } else {
                val d = ctx.getExternalFilesDir(null) ?: ctx.filesDir
                val f = File(d, name)
                logFile.copyTo(f, overwrite = true)
                "Salvo em ${f.absolutePath}"
            }
        } catch (e: Exception) {
            "Erro ao salvar: ${e.message}"
        }
    }
}
