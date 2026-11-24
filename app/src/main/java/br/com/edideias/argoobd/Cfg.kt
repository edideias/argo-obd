package br.com.edideias.argoobd

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class Cfg(ctx: Context) {
    val sp: SharedPreferences = ctx.getSharedPreferences("argo", Context.MODE_PRIVATE)

    /** 0 = gasolina (E30), 1 = etanol, 2 = mistura manual */
    var fuelType: Int
        get() = sp.getInt("fuelType", 0)
        set(v) { sp.edit().putInt("fuelType", v).apply() }

    var customEth: Int
        get() = sp.getInt("customEth", 50)
        set(v) { sp.edit().putInt("customEth", v).apply() }

    var priceGas: Float
        get() = sp.getFloat("priceGas", 6.20f)
        set(v) { sp.edit().putFloat("priceGas", v).apply() }

    var priceEth: Float
        get() = sp.getFloat("priceEth", 4.40f)
        set(v) { sp.edit().putFloat("priceEth", v).apply() }

    /** cilindrada em litros */
    var disp: Float
        get() = sp.getFloat("disp", 0.999f)
        set(v) { sp.edit().putFloat("disp", v).apply() }

    /** eficiência volumétrica estimada */
    var ve: Float
        get() = sp.getFloat("ve", 0.85f)
        set(v) { sp.edit().putFloat("ve", v).apply() }

    /** fator de calibração do consumo */
    var calib: Float
        get() = sp.getFloat("calib", 1.0f)
        set(v) { sp.edit().putFloat("calib", v).apply() }

    var lastDevice: String
        get() = sp.getString("lastDevice", "") ?: ""
        set(v) { sp.edit().putString("lastDevice", v).apply() }

    var lastDeviceName: String
        get() = sp.getString("lastDeviceName", "") ?: ""
        set(v) { sp.edit().putString("lastDeviceName", v).apply() }

    var demo: Boolean
        get() = sp.getBoolean("demo", false)
        set(v) { sp.edit().putBoolean("demo", v).apply() }

    var keepOn: Boolean
        get() = sp.getBoolean("keepOn", true)
        set(v) { sp.edit().putBoolean("keepOn", v).apply() }

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
    private var lastSave = 0L

    @Synchronized
    fun add(dt: Double, spd: Double, lh: Double, price: Double) {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
        secs += dt
        dist += spd * dt / 3600.0
        val l = lh * dt / 3600.0
        liters += l
        cost += l * price
        if (spd > maxSpd) maxSpd = spd
        val now = System.currentTimeMillis()
        if (now - lastSave > 10000) save()
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
            .apply()
    }

    fun kmPerL(): Double? = if (liters > 0.02 && dist > 0.05) dist / liters else null
    fun costPerKm(): Double? = if (dist > 0.1) cost / dist else null
    fun avgSpeed(): Double? = if (secs > 5) dist / (secs / 3600.0) else null

    @Synchronized
    fun reset() {
        dist = 0.0; liters = 0.0; cost = 0.0; secs = 0.0; maxSpd = 0.0; startedAt = 0L
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
