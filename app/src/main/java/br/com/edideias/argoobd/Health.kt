package br.com.edideias.argoobd

/** Diagnóstico rápido: lê valores ao vivo e dá um veredito em português. */
class Check(val title: String, val level: Int, val text: String) // 0 ok, 1 atenção, 2 problema, 3 sem dado

object Health {
    fun run(m: Manager): List<Check> {
        val out = ArrayList<Check>()
        val v = m.values
        val rpm = m.fresh(0x0C, 8000)
        val running = rpm != null && rpm > 400

        val volt = m.fresh(Pids.VOLT, 15000) ?: m.fresh(0x42, 15000)
        if (volt == null) out.add(Check("Bateria / alternador", 3, "Sem leitura de tensão."))
        else if (running) {
            when {
                volt < 13.2 -> out.add(Check("Bateria / alternador", 2, "${Fmt.num(volt, 1)} V com o motor ligado: o alternador não está carregando bem."))
                volt > 15.0 -> out.add(Check("Bateria / alternador", 2, "${Fmt.num(volt, 1)} V: tensão alta demais, risco para a bateria."))
                else -> out.add(Check("Bateria / alternador", 0, "${Fmt.num(volt, 1)} V com o motor ligado: carga normal."))
            }
        } else {
            when {
                volt < 11.8 -> out.add(Check("Bateria / alternador", 2, "${Fmt.num(volt, 1)} V com o motor desligado: bateria descarregada ou no fim da vida."))
                volt < 12.3 -> out.add(Check("Bateria / alternador", 1, "${Fmt.num(volt, 1)} V com o motor desligado: bateria meio fraca."))
                else -> out.add(Check("Bateria / alternador", 0, "${Fmt.num(volt, 1)} V com o motor desligado: bateria boa."))
            }
        }

        val ect = m.fresh(0x05, 8000)
        val ectMax = m.maxs[0x05]
        if (ect == null) out.add(Check("Temperatura do motor", 3, "Sem leitura."))
        else when {
            ect >= 108 -> out.add(Check("Temperatura do motor", 2, "${ect.toInt()} °C: superaquecendo. Pare em local seguro."))
            ect >= 100 -> out.add(Check("Temperatura do motor", 1, "${ect.toInt()} °C: alta. Observe o ventilador e o nível do arrefecimento."))
            ect < 70 && ectMax != null && ectMax < 70 -> out.add(Check("Temperatura do motor", 1, "${ect.toInt()} °C: ainda aquecendo (ou termostato aberto demais se já rodou bastante)."))
            else -> out.add(Check("Temperatura do motor", 0, "${ect.toInt()} °C: dentro do normal."))
        }

        val st = m.fresh(0x06, 8000)
        val lt = m.fresh(0x07, 8000)
        if (st == null && lt == null) out.add(Check("Correção de combustível", 3, "O carro não informa as correções (PIDs 06/07)."))
        else {
            val worst = maxOf(Math.abs(st ?: 0.0), Math.abs(lt ?: 0.0))
            val txt = "curto prazo ${Fmt.num(st, 1)}% · longo prazo ${Fmt.num(lt, 1)}%"
            when {
                worst > 15 -> out.add(Check("Correção de combustível", 2, "$txt. Muito fora de zero: falsa entrada de ar, bico, sonda ou combustível ruim."))
                worst > 8 -> out.add(Check("Correção de combustível", 1, "$txt. A ECU está compensando bastante."))
                else -> out.add(Check("Correção de combustível", 0, "$txt. Mistura equilibrada."))
            }
        }

        val map = m.fresh(0x0B, 8000)
        if (map != null && running && (m.fresh(0x0D) ?: 0.0) < 2.0 && rpm != null && rpm < 1100) {
            when {
                map > 55 -> out.add(Check("Vácuo na marcha lenta", 2, "${map.toInt()} kPa: alto demais. Suspeita de vazamento de vácuo ou motor falhando."))
                map > 45 -> out.add(Check("Vácuo na marcha lenta", 1, "${map.toInt()} kPa: um pouco alto."))
                else -> out.add(Check("Vácuo na marcha lenta", 0, "${map.toInt()} kPa: normal."))
            }
        }

        if (rpm != null && running && (m.fresh(0x0D) ?: 0.0) < 2.0) {
            when {
                rpm > 1200 && (ect ?: 0.0) > 85 -> out.add(Check("Marcha lenta", 1, "${rpm.toInt()} rpm com o motor quente: alta. Veja a borboleta e a entrada de ar."))
                rpm < 600 -> out.add(Check("Marcha lenta", 2, "${rpm.toInt()} rpm: baixa, risco de apagar."))
                else -> out.add(Check("Marcha lenta", 0, "${rpm.toInt()} rpm."))
            }
        }

        val l2 = m.fresh(0x14, 8000) ?: m.fresh(0x24, 8000)
        if (l2 != null && running) {
            out.add(Check("Sonda lambda", 0, "Respondendo (${Fmt.num(l2, 2)})."))
        }

        out.add(
            if (m.milOn) Check("Luz de injeção (MIL)", 2, "Acesa, com ${m.dtcCount} falha(s) registrada(s). Veja a aba Falhas.")
            else Check("Luz de injeção (MIL)", 0, "Apagada.")
        )
        return out
    }
}
