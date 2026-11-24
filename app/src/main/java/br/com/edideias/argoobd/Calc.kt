package br.com.edideias.argoobd

object Calc {
    private fun afr(e: Double) = 14.7 * (1 - e) + 9.0 * e
    private fun density(e: Double) = 745.0 * (1 - e) + 789.0 * e // g/L

    /**
     * Consumo estimado em litros por hora.
     * Prefere a taxa informada pela ECU (5E); depois MAF; por fim estima pelo MAP (densidade-velocidade).
     */
    fun fuelLh(v: Map<Int, Double>, cfg: Cfg): Double? {
        val e = v[0x52]?.let { (it / 100.0).coerceIn(0.0, 1.0) } ?: cfg.ethFrac()
        val k = cfg.calib.toDouble()
        v[0x5E]?.let { return it * k }
        val maf = v[0x10]
        if (maf != null) {
            return maf / afr(e) * 3600.0 / density(e) * k
        }
        val rpm = v[0x0C]
        val map = v[0x0B]
        val iat = v[0x0F] ?: 30.0
        if (rpm != null && map != null) {
            val tk = iat + 273.15
            val air = map * cfg.disp * cfg.ve / (8.314 * tk) * 28.97 * (rpm / 120.0) // g/s
            return air / afr(e) * 3600.0 / density(e) * k
        }
        return null
    }

    fun method(v: Map<Int, Double>): String = when {
        v.containsKey(0x5E) -> "taxa de combustível da ECU"
        v.containsKey(0x10) -> "sensor de fluxo de ar (MAF)"
        v.containsKey(0x0B) -> "estimativa pela pressão do coletor (MAP)"
        else -> "sem dados suficientes"
    }
}
