package br.com.edideias.argoobd

class AccelTest(val key: String, val label: String, val from: Int, val to: Int, val quarter: Boolean = false, val brake: Boolean = false)

/** Cronômetro de desempenho a partir da velocidade do carro (PID 0D). Use só em pista fechada. */
class Accel(private val cfg: Cfg) {
    companion object {
        val tests = listOf(
            AccelTest("0_100", "0 a 100 km/h", 0, 100),
            AccelTest("0_60", "0 a 60 km/h", 0, 60),
            AccelTest("40_80", "40 a 80 km/h", 40, 80),
            AccelTest("60_100", "60 a 100 km/h (retomada)", 60, 100),
            AccelTest("400m", "400 metros (¼ de milha)", 0, 0, quarter = true),
            AccelTest("100_0", "Frenagem 100 a 0 km/h", 100, 0, brake = true)
        )
    }

    // 0 parado, 1 armado (esperando a largada), 2 correndo, 3 concluído
    @Volatile var state = 0
    @Volatile var test: AccelTest = tests[0]
    @Volatile var elapsed = 0.0
    @Volatile var dist = 0.0
    @Volatile var result: Double? = null
    @Volatile var resultSpeed: Double? = null
    @Volatile var record = false

    private var t0 = 0L
    private var lastT = 0L
    private var lastV = 0.0
    private var peak = 0.0

    fun active() = state == 1 || state == 2

    fun arm(t: AccelTest) {
        test = t
        state = 1
        elapsed = 0.0
        dist = 0.0
        result = null
        resultSpeed = null
        record = false
        lastT = 0L
        lastV = 0.0
        peak = 0.0
    }

    fun cancel() {
        state = 0
    }

    fun feed(v: Double, now: Long) {
        val st = state
        if (st == 0 || st == 3) return
        val prevT = lastT
        val prevV = lastV
        lastT = now
        lastV = v
        if (prevT == 0L) return
        val t = test
        if (st == 1) {
            if (v > peak) peak = v
            val go = when {
                t.brake -> peak >= t.from && v <= peak - 3
                t.from == 0 -> prevV <= 1.0 && v >= 2.0
                else -> prevV < t.from && v >= t.from
            }
            if (go) {
                t0 = if (t.from == 0 && !t.brake) prevT else now
                dist = 0.0
                state = 2
            }
            return
        }
        // correndo
        val dt = (now - prevT) / 1000.0
        dist += (v + prevV) / 2.0 / 3.6 * dt
        elapsed = (now - t0) / 1000.0
        if (elapsed > 60.0) { state = 0; return }
        if (t.brake) {
            if (v <= 1.5) finish(elapsed, null)
            return
        }
        if (t.quarter) {
            if (dist >= 402.34) {
                val over = dist - 402.34
                val speedMs = (v / 3.6).coerceAtLeast(1.0)
                finish(elapsed - over / speedMs, v)
            }
            return
        }
        if (v >= t.to) {
            val frac = if (v > prevV) (t.to - prevV) / (v - prevV) else 1.0
            val tt = (prevT - t0) / 1000.0 + frac * dt
            finish(tt, null)
        }
        if (v < prevV - 6 && elapsed > 1.0) state = 0 // tirou o pé: cancela
    }

    private fun finish(sec: Double, spd: Double?) {
        result = sec
        resultSpeed = spd
        elapsed = sec
        state = 3
        val k = test.key
        val best = cfg.bestTime(k)
        if (best == null || sec < best) {
            cfg.setBest(k, sec)
            record = true
        }
    }
}
