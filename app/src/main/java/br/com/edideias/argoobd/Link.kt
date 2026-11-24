package br.com.edideias.argoobd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

interface Link {
    fun open()
    fun query(cmd: String, timeoutMs: Long = 2500): String
    fun close()
}

/** Bluetooth clássico (SPP) – o tipo do ELM327 Mini azul. */
class BtLink(private val device: BluetoothDevice) : Link {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    @SuppressLint("MissingPermission")
    override fun open() {
        val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var s: BluetoothSocket? = null
        try {
            s = device.createRfcommSocketToServiceRecord(spp)
            s.connect()
        } catch (e: Exception) {
            try { s?.close() } catch (_: Exception) {}
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            s = m.invoke(device, 1) as BluetoothSocket
            s.connect()
        }
        socket = s
        input = s!!.inputStream
        output = s.outputStream
    }

    override fun query(cmd: String, timeoutMs: Long): String {
        val o = output ?: throw IOException("desconectado")
        val i = input ?: throw IOException("desconectado")
        while (i.available() > 0) i.read()
        o.write((cmd + "\r").toByteArray())
        o.flush()
        val sb = StringBuilder()
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (i.available() > 0) {
                val c = i.read()
                if (c < 0) throw IOException("fim da conexão")
                val ch = c.toChar()
                if (ch == '>') return sb.toString()
                sb.append(ch)
            } else {
                Thread.sleep(4)
            }
        }
        return sb.toString() + "\nTIMEOUT"
    }

    override fun close() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}

/** Carro simulado: responde como um ELM327 de verdade, para ver o app funcionando sem o leitor. */
class DemoLink : Link {
    private val t0 = System.currentTimeMillis()
    private var cleared = false

    private val sup = setOf(
        0x04, 0x05, 0x06, 0x07, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x1F, 0x21,
        0x2F, 0x31, 0x33, 0x42, 0x43, 0x44, 0x45, 0x46, 0x52, 0x5C
    )

    override fun open() {}
    override fun close() {}

    private fun hex(vararg b: Int) = b.joinToString(" ") { "%02X".format(it and 0xFF) }
    private fun u16(v: Int) = intArrayOf((v shr 8) and 0xFF, v and 0xFF)

    private fun encode(pid: Int): IntArray? {
        val t = (System.currentTimeMillis() - t0) / 1000.0
        val v = max(0.0, 38 + 46 * sin(t / 11.0))
        val acc = 46 / 11.0 * kotlin.math.cos(t / 11.0)
        val ratio = when {
            v < 15 -> 105.0
            v < 35 -> 66.0
            v < 55 -> 46.0
            v < 80 -> 36.0
            else -> 29.0
        }
        val rpm = if (v < 1.5) 780 + 20 * sin(t * 3) else max(800.0, v * ratio)
        val load = min(95.0, max(15.0, 18 + v * 0.25 + max(0.0, acc) * 6))
        val map = 28 + load * 0.62
        val thr = 12 + load * 0.45
        val ect = min(91.0, 45 + t * 0.5)
        val maf = (rpm * map / (307 * 120.0)) * 0.85 * 0.999 * 28.97 / 8.314
        return when (pid) {
            0x04 -> intArrayOf((load * 255 / 100).toInt())
            0x05 -> intArrayOf((ect + 40).toInt())
            0x06 -> intArrayOf((128 + 3 * sin(t)).toInt())
            0x07 -> intArrayOf((128 + 2 * sin(t / 5)).toInt())
            0x0B -> intArrayOf(map.toInt())
            0x0C -> u16((rpm * 4).toInt())
            0x0D -> intArrayOf(v.toInt())
            0x0E -> intArrayOf((((12 + load * 0.15) + 64) * 2).toInt())
            0x0F -> intArrayOf(34 + 40)
            0x10 -> u16((maf * 100).toInt())
            0x11 -> intArrayOf((thr * 255 / 100).toInt())
            0x1F -> u16(t.toInt())
            0x21 -> u16(if (cleared) 0 else 12)
            0x2F -> intArrayOf(153)
            0x31 -> u16(if (cleared) 0 else 1234)
            0x33 -> intArrayOf(100)
            0x42 -> u16((14000 + 150 * sin(t / 3)).toInt())
            0x43 -> u16((load * 255 / 100).toInt())
            0x44 -> u16(32768)
            0x45 -> intArrayOf((thr * 255 / 100 * 0.6).toInt())
            0x46 -> intArrayOf(30 + 40)
            0x52 -> intArrayOf(76)
            0x5C -> intArrayOf((ect + 5 + 40).toInt())
            else -> null
        }
    }

    private fun mask(base: Int): String {
        var m = 0L
        for (i in 0 until 32) if ((base + i + 1) in sup) m = m or (1L shl (31 - i))
        if (sup.any { it > base + 32 }) m = m or 1L
        return "41 %02X %02X %02X %02X %02X".format(
            base, (m shr 24) and 0xFF, (m shr 16) and 0xFF, (m shr 8) and 0xFF, m and 0xFF
        )
    }

    private var headers = false

    override fun query(cmd: String, timeoutMs: Long): String {
        Thread.sleep(30)
        val c = cmd.uppercase().replace(" ", "")
        if (c.startsWith("AT")) {
            return when {
                c == "ATZ" || c == "ATI" -> "ELM327 v1.5 (demo)"
                c == "ATDP" -> "ISO 15765-4 (CAN 11/500)"
                c == "ATRV" -> "13.9V"
                c == "AT@1" -> "OBDII to RS232 Interpreter"
                c == "ATH1" -> { headers = true; "OK" }
                c == "ATH0" -> { headers = false; "OK" }
                else -> "OK"
            }
        }
        // consulta de modo 01 (um ou vários PIDs, com dígito final opcional de contagem)
        if (c.length >= 4 && c.startsWith("01")) {
            var body = c.substring(2)
            if (body.length % 2 == 1) body = body.dropLast(1)
            val pids = body.chunked(2).mapNotNull { it.toIntOrNull(16) }
            if (pids.size == 1) {
                val p = pids[0]
                if (p == 0x00 || p == 0x20 || p == 0x40 || p == 0x60) {
                    val m = mask(p)
                    return if (headers && p == 0x00) "7E8 06 $m" else m
                }
                if (p == 0x01) return if (cleared) "41 01 00 07 65 00" else "41 01 81 07 65 04"
            }
            val sb = StringBuilder("41")
            var any = false
            for (p in pids) {
                val d = if (p in sup) encode(p) else null
                if (d != null) {
                    sb.append(" %02X ".format(p)).append(hex(*d))
                    any = true
                }
            }
            return if (any) sb.toString() else "NO DATA"
        }
        if (c.length == 6 && c.startsWith("02") && c.endsWith("00")) {
            val p = c.substring(2, 4).toIntOrNull(16) ?: return "?"
            if (p == 0x02) return if (cleared) "NO DATA" else "42 02 00 01 71"
            val d = if (p in sup) encode(p) else null
            return if (d == null) "NO DATA" else "42 %02X 00 ".format(p) + hex(*d)
        }
        // modo 06
        if (c.length == 4 && c.startsWith("06")) {
            val m = c.substring(2).toIntOrNull(16) ?: return "?"
            val mids = setOf(0x01, 0x02, 0x21, 0xA1, 0xA2, 0xA3, 0xA4)
            if (m == 0x00 || m == 0x20 || m == 0x40 || m == 0x60 || m == 0x80 || m == 0xA0 || m == 0xC0) {
                var mk = 0L
                for (i in 0 until 32) if ((m + i + 1) in mids) mk = mk or (1L shl (31 - i))
                if (mids.any { it > m + 32 }) mk = mk or 1L
                return "46 %02X %02X %02X %02X %02X".format(m, (mk shr 24) and 0xFF, (mk shr 16) and 0xFF, (mk shr 8) and 0xFF, mk and 0xFF)
            }
            if (m !in mids) return "NO DATA"
            val n = (System.currentTimeMillis() / 1000 % 7).toInt()
            return when (m) {
                0x01 -> "46 01 0A 0B %02X %02X 00 00 00 FF".format(0x01, 0x40 + n)
                0x02 -> "46 02 0A 0B %02X %02X 00 00 00 FF".format(0x00, 0x50 + n)
                0x21 -> "46 21 A0 0B 03 E8 00 00 07 D0"
                else -> "46 %02X 0B 0C 00 00 00 00 00 0A".format(m)
            }
        }
        // UDS (serviço 22)
        if (c.length == 6 && c.startsWith("22")) {
            val did = c.substring(2).toIntOrNull(16) ?: return "?"
            fun txt(t: String) = "62 %02X %02X ".format(did shr 8, did and 0xFF) + t.map { "%02X".format(it.code) }.joinToString(" ")
            return when (did) {
                0xF190 -> txt("9BDDEMO0000000001")
                0xF187 -> txt("DEMO-PN-5206")
                0xF189 -> txt("SW 11GF.2026")
                0xF191 -> txt("HW DEMO 1.0")
                0xF18C -> txt("SN0000DEMO")
                else -> "7F 22 31"
            }
        }
        return when (c) {
            "03" -> if (cleared) "43 00" else "43 01 01 71"
            "07" -> "47 00"
            "0A" -> "4A 00"
            "04" -> { cleared = true; "44" }
            "0902" -> "49 02 01 39 42 44 44 45 4D 4F 30 30 30 30 30 30 30 30 31"
            "0904" -> "49 04 01 44 45 4D 4F 2D 43 41 4C"
            "090A" -> "49 0A 01 45 43 4D 2D 4D 41 52 45 4C 4C 49"
            else -> "NO DATA"
        }
    }
}
