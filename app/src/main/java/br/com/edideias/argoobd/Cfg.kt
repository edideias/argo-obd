package br.com.edideias.argoobd

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class MaintItem(var name: String, var intervalKm: Int, var lastKm: Double)

class Cfg(ctx: Context) {
    val sp: SharedPreferences = ctx.getSharedPreferences("argo", Context.MODE_PRIVATE)

    private fun f(k: String, d: Float) = sp.getFloat(k, d)
    private fun putF(k: String, v: Float) { sp.edit().putFloat(k, v).apply() }

    /** 0 = gasolina (E30), 1 = etanol, 2 = mistura manual */
    var fuelType: Int
        get() = sp.getInt("fuelType", 0)
        set(v) { sp.edit().putInt("fuelType", v).apply() }

    var customEth: Int
        get() = sp.getInt("customEth", 50)
        set(v) { sp.edit().putInt("customEth", v).apply() }

    var priceGas: Float
        get() = f("priceGas", 6.20f)
        set(v) = putF("priceGas", v)

    var priceEth: Float
        get() = f("priceEth", 4.40f)
        set(v) = putF("priceEth", v)

    /** cilindrada em litros */
    var disp: Float
        get() = f("disp", 0.999f)
        set(v) = putF("disp", v)

    /** eficiência volumétrica estimada */
    var ve: Float
        get() = f("ve", 0.85f)
        set(v) = putF("ve", v)

    /** fator de calibração do consumo */
    var calib: Float
        get() = f("calib", 1.0f)
        set(v) = putF("calib", v)

    /** capacidade do tanque (litros) */
    var tank: Float
        get() = f("tank", 48f)
        set(v) = putF("tank", v)

    var lastDevice: String
        get() = sp.getString("lastDevice", "") ?: ""
        set(v) { sp.edit().putString("lastDevice", v).apply() }

    var lastDeviceName: String
        get() = sp.getString("lastDeviceName", "") ?: ""
        set(v) { sp.edit().putString("lastDeviceName", v).apply() }

    var demo: Boolean
        get() = sp.getBoolean("demo", false)
        set(v) { sp.edit().putBoolean("demo", v).apply() }

    /** 0 Carbono, 1 Hi-Tech, 2 Clássico */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }

    var keepOn: Boolean
        get() = sp.getBoolean("keepOn", true)
        set(v) { sp.edit().putBoolean("keepOn", v).apply() }

    // ---- alertas ----
    var alertsOn: Boolean
        get() = sp.getBoolean("alertsOn", true)
        set(v) { sp.edit().putBoolean("alertsOn", v).apply() }

    var tempMax: Int
        get() = sp.getInt("tempMax", 105)
        set(v) { sp.edit().putInt("tempMax", v).apply() }

    var voltMin: Float
        get() = f("voltMin", 12.0f)
        set(v) = putF("voltMin", v)

    /** 0 = desligado */
    var rpmMax: Int
        get() = sp.getInt("rpmMax", 0)
        set(v) { sp.edit().putInt("rpmMax", v).apply() }

    /** 0 = desligado */
    var speedMax: Int
        get() = sp.getInt("speedMax", 0)
        set(v) { sp.edit().putInt("speedMax", v).apply() }

    // ---- painel ----
    var motorPids: List<Int>
        get() {
            val s = sp.getString("motorPids", null) ?: return listOf(0x05, 0x04, 0x11, 0x0B, 0x0F, 0x0E, 0x06, 0x07)
            return s.split(',').mapNotNull { it.toIntOrNull() }
        }
        set(v) { sp.edit().putString("motorPids", v.joinToString(",")).apply() }

    /** quanto o motorista fatura por km rodado (R$) */
    var earnKm: Float
        get() = f("earnKm", 0f)
        set(v) = putF("earnKm", v)

    // ---- hodômetro e totais ----
    var odo: Float
        get() = f("odo", 0f)
        set(v) = putF("odo", v)

    var lifeDist: Float
        get() = f("lifeDist", 0f)
        set(v) = putF("lifeDist", v)

    var lifeLiters: Float
        get() = f("lifeLiters", 0f)
        set(v) = putF("lifeLiters", v)

    fun maint(): MutableList<MaintItem> {
        val s = sp.getString("maint", null)
        if (s == null) {
            return mutableListOf(
                MaintItem("Troca de óleo e filtro", 10000, 0.0),
                MaintItem("Filtro de ar do motor", 20000, 0.0),
                MaintItem("Velas de ignição", 30000, 0.0),
                MaintItem("Fluido de freio", 20000, 0.0),
                MaintItem("Filtro do ar-condicionado", 10000, 0.0)
            )
        }
        val arr = try { JSONArray(s) } catch (e: Exception) { JSONArray() }
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            MaintItem(o.optString("n"), o.optInt("i", 10000), o.optDouble("l", 0.0))
        }.toMutableList()
    }

    fun saveMaint(list: List<MaintItem>) {
        val arr = JSONArray()
        for (m in list) {
            arr.put(JSONObject().put("n", m.name).put("i", m.intervalKm).put("l", m.lastKm))
        }
        sp.edit().putString("maint", arr.toString()).apply()
    }

    // ---- testes de aceleração ----
    fun bestTime(key: String): Double? {
        val v = sp.getFloat("best_$key", 0f)
        return if (v > 0f) v.toDouble() else null
    }

    fun setBest(key: String, v: Double) {
        sp.edit().putFloat("best_$key", v.toFloat()).apply()
    }

    /** fração de etanol assumida quando a ECU não informa */
    fun ethFrac(): Double = when (fuelType) {
        0 -> 0.30
        1 -> 1.0
        else -> customEth.coerceIn(0, 100) / 100.0
    }

    fun price(): Double {
        val f = when (fuelType) {
            0 -> 0.0
            1 -> 1.0
            else -> customEth.coerceIn(0, 100) / 100.0
        }
        return priceGas * (1 - f) + priceEth * f
    }

    fun fuelName(): String = when (fuelType) {
        0 -> "Gasolina"
        1 -> "Etanol"
        else -> "Mistura ${customEth}% etanol"
    }
}

class Trip(private val sp: SharedPreferences) {
    @Volatile var dist = sp.getFloat("t_dist", 0f).toDouble()
    @Volatile var liters = sp.getFloat("t_liters", 0f).toDouble()
    @Volatile var cost = sp.getFloat("t_cost", 0f).toDouble()
    @Volatile var secs = sp.getFloat("t_secs", 0f).toDouble()
    @Volatile var maxSpd = sp.getFloat("t_max", 0f).toDouble()
    @Volatile var startedAt = sp.getLong("t_start", 0L)
    @Volatile var hardAcc = sp.getInt("t_hacc", 0)
    @Volatile var hardBrk = sp.getInt("t_hbrk", 0)
    @Volatile var hiRpmSecs = sp.getFloat("t_hirpm", 0f).toDouble()
    @Volatile var idleSecs = sp.getFloat("t_idle", 0f).toDouble()
    private var lastSave = 0L

    @Synchronized
    fun add(dt: Double, spd: Double, rpm: Double, lh: Double, price: Double) {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
        secs += dt
        dist += spd * dt / 3600.0
        val l = lh * dt / 3600.0
        liters += l
        cost += l * price
        if (spd > maxSpd) maxSpd = spd
        if (rpm > 3500) hiRpmSecs += dt
        if (spd < 2) idleSecs += dt
        val now = System.currentTimeMillis()
        if (now - lastSave > 10000) save()
    }

    fun ecoScore(): Int? {
        if (secs < 60 || dist < 0.3) return null
        val perKm = dist.coerceAtLeast(0.5)
        val s = 100.0 - (hardAcc * 4.0 + hardBrk * 5.0) / perKm * 3.0 - (hiRpmSecs / secs) * 80.0
        return s.coerceIn(0.0, 100.0).toInt()
    }

    @Synchronized
    fun save() {
        lastSave = System.currentTimeMillis()
        sp.edit()
            .putFloat("t_dist", dist.toFloat())
            .putFloat("t_liters", liters.toFloat())
            .putFloat("t_cost", cost.toFloat())
            .putFloat("t_secs", secs.toFloat())
            .putFloat("t_max", maxSpd.toFloat())
            .putLong("t_start", startedAt)
            .putInt("t_hacc", hardAcc)
            .putInt("t_hbrk", hardBrk)
            .putFloat("t_hirpm", hiRpmSecs.toFloat())
            .putFloat("t_idle", idleSecs.toFloat())
            .apply()
    }

    fun kmPerL(): Double? = if (liters > 0.02 && dist > 0.05) dist / liters else null
    fun costPerKm(): Double? = if (dist > 0.1) cost / dist else null
    fun avgSpeed(): Double? = if (secs > 5) dist / (secs / 3600.0) else null

    @Synchronized
    fun reset() {
        dist = 0.0; liters = 0.0; cost = 0.0; secs = 0.0; maxSpd = 0.0; startedAt = 0L
        hardAcc = 0; hardBrk = 0; hiRpmSecs = 0.0; idleSecs = 0.0
        save()
    }

    @Synchronized
    fun saveToHistory(fuel: String) {
        if (dist < 0.05) return
        val arr = try { JSONArray(sp.getString("trips", "[]")) } catch (e: Exception) { JSONArray() }
        val o = JSONObject()
        o.put("start", if (startedAt == 0L) System.currentTimeMillis() else startedAt)
        o.put("dist", dist)
        o.put("liters", liters)
        o.put("cost", cost)
        o.put("secs", secs)
        o.put("max", maxSpd)
        o.put("fuel", fuel)
        o.put("eco", ecoScore() ?: -1)
        val out = JSONArray()
        out.put(o)
        for (i in 0 until minOf(arr.length(), 99)) out.put(arr.get(i))
        sp.edit().putString("trips", out.toString()).apply()
    }

    fun history(): List<JSONObject> {
        val arr = try { JSONArray(sp.getString("trips", "[]")) } catch (e: Exception) { JSONArray() }
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    fun clearHistory() {
        sp.edit().putString("trips", "[]").apply()
    }
}
