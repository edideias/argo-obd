package br.com.edideias.argoobd

object Dtc {
    private val table: Map<String, String> = mapOf(
        "P0016" to "Correlação virabrequim x comando de válvulas (sincronismo)",
        "P0030" to "Aquecedor da sonda lambda B1S1 – circuito",
        "P0031" to "Aquecedor da sonda lambda B1S1 – tensão baixa",
        "P0032" to "Aquecedor da sonda lambda B1S1 – tensão alta",
        "P0036" to "Aquecedor da sonda lambda B1S2 – circuito",
        "P0037" to "Aquecedor da sonda lambda B1S2 – tensão baixa",
        "P0038" to "Aquecedor da sonda lambda B1S2 – tensão alta",
        "P0100" to "Sensor de fluxo de ar (MAF) – circuito",
        "P0101" to "Sensor de fluxo de ar (MAF) – fora da faixa",
        "P0102" to "Sensor de fluxo de ar (MAF) – sinal baixo",
        "P0103" to "Sensor de fluxo de ar (MAF) – sinal alto",
        "P0105" to "Sensor de pressão do coletor (MAP) – circuito",
        "P0106" to "Sensor de pressão do coletor (MAP) – fora da faixa",
        "P0107" to "Sensor de pressão do coletor (MAP) – sinal baixo",
        "P0108" to "Sensor de pressão do coletor (MAP) – sinal alto",
        "P0110" to "Sensor de temperatura do ar – circuito",
        "P0111" to "Sensor de temperatura do ar – fora da faixa",
        "P0112" to "Sensor de temperatura do ar – sinal baixo",
        "P0113" to "Sensor de temperatura do ar – sinal alto",
        "P0115" to "Sensor de temperatura da água – circuito",
        "P0116" to "Sensor de temperatura da água – fora da faixa",
        "P0117" to "Sensor de temperatura da água – sinal baixo",
        "P0118" to "Sensor de temperatura da água – sinal alto",
        "P0120" to "Sensor de posição do acelerador (TPS) A – circuito",
        "P0121" to "Sensor de posição do acelerador (TPS) A – fora da faixa",
        "P0122" to "Sensor de posição do acelerador (TPS) A – sinal baixo",
        "P0123" to "Sensor de posição do acelerador (TPS) A – sinal alto",
        "P0125" to "Motor demorou a esquentar para a injeção em malha fechada",
        "P0128" to "Temperatura do motor abaixo da esperada (válvula termostática)",
        "P0130" to "Sonda lambda B1S1 – circuito",
        "P0131" to "Sonda lambda B1S1 – tensão baixa",
        "P0132" to "Sonda lambda B1S1 – tensão alta",
        "P0133" to "Sonda lambda B1S1 – resposta lenta",
        "P0134" to "Sonda lambda B1S1 – sem atividade",
        "P0135" to "Sonda lambda B1S1 – aquecedor com defeito",
        "P0136" to "Sonda lambda B1S2 (pós-catalisador) – circuito",
        "P0137" to "Sonda lambda B1S2 – tensão baixa",
        "P0138" to "Sonda lambda B1S2 – tensão alta",
        "P0139" to "Sonda lambda B1S2 – resposta lenta",
        "P0140" to "Sonda lambda B1S2 – sem atividade",
        "P0141" to "Sonda lambda B1S2 – aquecedor com defeito",
        "P0171" to "Mistura pobre (excesso de ar ou falta de combustível)",
        "P0172" to "Mistura rica (excesso de combustível)",
        "P0201" to "Bico injetor do cilindro 1 – circuito",
        "P0202" to "Bico injetor do cilindro 2 – circuito",
        "P0203" to "Bico injetor do cilindro 3 – circuito",
        "P0217" to "Superaquecimento do motor",
        "P0220" to "Sensor do acelerador B – circuito",
        "P0222" to "Sensor do acelerador B – sinal baixo",
        "P0223" to "Sensor do acelerador B – sinal alto",
        "P0230" to "Bomba de combustível – circuito do relé",
        "P0261" to "Bico injetor do cilindro 1 – sinal baixo",
        "P0264" to "Bico injetor do cilindro 2 – sinal baixo",
        "P0267" to "Bico injetor do cilindro 3 – sinal baixo",
        "P0300" to "Falha de ignição aleatória / múltiplos cilindros",
        "P0301" to "Falha de ignição no cilindro 1",
        "P0302" to "Falha de ignição no cilindro 2",
        "P0303" to "Falha de ignição no cilindro 3",
        "P0325" to "Sensor de detonação – circuito",
        "P0327" to "Sensor de detonação – sinal baixo",
        "P0328" to "Sensor de detonação – sinal alto",
        "P0335" to "Sensor de rotação (virabrequim) – circuito",
        "P0336" to "Sensor de rotação (virabrequim) – fora da faixa",
        "P0337" to "Sensor de rotação (virabrequim) – sinal baixo",
        "P0338" to "Sensor de rotação (virabrequim) – sinal alto",
        "P0340" to "Sensor de fase (comando) – circuito",
        "P0341" to "Sensor de fase (comando) – fora da faixa",
        "P0351" to "Bobina de ignição do cilindro 1 – circuito",
        "P0352" to "Bobina de ignição do cilindro 2 – circuito",
        "P0353" to "Bobina de ignição do cilindro 3 – circuito",
        "P0420" to "Eficiência do catalisador abaixo do limite",
        "P0440" to "Sistema de emissões evaporativas (EVAP) – falha",
        "P0441" to "EVAP – fluxo de purga incorreto",
        "P0442" to "EVAP – vazamento pequeno (verifique a tampa do tanque)",
        "P0443" to "Válvula de purga do canister – circuito",
        "P0444" to "Válvula de purga do canister – aberta",
        "P0445" to "Válvula de purga do canister – em curto",
        "P0455" to "EVAP – vazamento grande (tampa do tanque solta?)",
        "P0456" to "EVAP – vazamento muito pequeno",
        "P0480" to "Ventilador de arrefecimento – circuito",
        "P0500" to "Sensor de velocidade do veículo – falha",
        "P0501" to "Sensor de velocidade do veículo – fora da faixa",
        "P0504" to "Interruptor do freio – sinais divergentes",
        "P0505" to "Controle da marcha lenta – falha",
        "P0506" to "Marcha lenta abaixo do esperado",
        "P0507" to "Marcha lenta acima do esperado",
        "P0560" to "Tensão do sistema elétrico – falha",
        "P0562" to "Tensão do sistema elétrico – baixa (bateria/alternador)",
        "P0563" to "Tensão do sistema elétrico – alta",
        "P0600" to "Comunicação serial da ECU – falha",
        "P0601" to "Memória interna da ECU – erro",
        "P0602" to "ECU – erro de programação",
        "P0606" to "Processador da ECU – falha",
        "P0620" to "Alternador – circuito de controle",
        "P0627" to "Bomba de combustível – circuito aberto",
        "P0630" to "VIN não programado na ECU",
        "P0638" to "Atuador da borboleta – fora da faixa",
        "P0641" to "Tensão de referência do sensor A – circuito",
        "P0651" to "Tensão de referência do sensor B – circuito",
        "P0685" to "Relé principal da ECU – circuito",
        "P0700" to "Sistema de controle da transmissão – falha",
        "P2100" to "Motor da borboleta eletrônica – circuito",
        "P2101" to "Motor da borboleta eletrônica – desempenho",
        "P2119" to "Borboleta eletrônica – faixa de operação",
        "P2135" to "Sensores do acelerador A/B – correlação",
        "P2138" to "Sensores do pedal D/E – correlação",
        "P2187" to "Sistema muito pobre em marcha lenta",
        "P2188" to "Sistema muito rico em marcha lenta",
        "P2195" to "Sonda lambda B1S1 – travada em pobre",
        "P2196" to "Sonda lambda B1S1 – travada em rica",
        "U0001" to "Rede CAN de alta velocidade – falha",
        "U0100" to "Sem comunicação com a ECU do motor",
        "U0121" to "Sem comunicação com o módulo do ABS",
        "U0140" to "Sem comunicação com o módulo da carroceria (BCM)",
        "U0155" to "Sem comunicação com o painel de instrumentos"
    )


    private val extraRaw = """
P0010|Atuador do comando de válvulas (admissão) – circuito
P0011|Comando de válvulas (admissão) – avançado em excesso
P0012|Comando de válvulas (admissão) – atrasado
P0013|Atuador do comando de válvulas (escapamento) – circuito
P0014|Comando de válvulas (escapamento) – avançado em excesso
P0017|Correlação virabrequim x comando (escapamento)
P0018|Correlação virabrequim x comando B2
P0021|Comando de válvulas B2 – avançado
P0033|Válvula de alívio do turbo – circuito
P0045|Válvula de controle do turbo – circuito
P0046|Controle do turbo – faixa de operação
P0047|Controle do turbo – sinal baixo
P0048|Controle do turbo – sinal alto
P0069|Pressão atmosférica x pressão do coletor – correlação
P0070|Sensor de temperatura ambiente – circuito
P0071|Sensor de temperatura ambiente – faixa
P0072|Sensor de temperatura ambiente – sinal baixo
P0073|Sensor de temperatura ambiente – sinal alto
P0074|Sensor de temperatura ambiente – intermitente
P0087|Pressão do trilho de combustível muito baixa
P0088|Pressão do trilho de combustível muito alta
P0089|Regulador de pressão de combustível – desempenho
P0090|Regulador de pressão de combustível – circuito
P0091|Regulador de pressão de combustível – sinal baixo
P0092|Regulador de pressão de combustível – sinal alto
P0104|Sensor de fluxo de ar (MAF) – intermitente
P0109|Sensor de pressão do coletor (MAP) – intermitente
P0114|Sensor de temperatura do ar – intermitente
P0119|Sensor de temperatura da água – intermitente
P0124|Sensor do acelerador A – intermitente
P0129|Sensor de pressão barométrica – fora da faixa
P0150|Sonda lambda B2S1 – circuito
P0151|Sonda lambda B2S1 – tensão baixa
P0152|Sonda lambda B2S1 – tensão alta
P0153|Sonda lambda B2S1 – resposta lenta
P0154|Sonda lambda B2S1 – sem atividade
P0155|Sonda lambda B2S1 – aquecedor com defeito
P0170|Correção de combustível banco 1 – falha
P0173|Correção de combustível banco 2 – falha
P0174|Mistura pobre banco 2
P0175|Mistura rica banco 2
P0190|Sensor de pressão do trilho – circuito
P0191|Sensor de pressão do trilho – faixa
P0192|Sensor de pressão do trilho – sinal baixo
P0193|Sensor de pressão do trilho – sinal alto
P0204|Bico injetor do cilindro 4 – circuito
P0218|Superaquecimento do câmbio
P0234|Turbo – sobrepressão
P0236|Sensor de pressão do turbo – faixa
P0243|Válvula de alívio do turbo A – circuito
P0299|Turbo – subpressão
P0304|Falha de ignição no cilindro 4
P0320|Sensor de rotação do motor – circuito
P0321|Sensor de rotação do motor – faixa
P0322|Sensor de rotação do motor – sem sinal
P0324|Sistema de controle de detonação – falha
P0330|Sensor de detonação 2 – circuito
P0339|Sensor de rotação (virabrequim) – intermitente
P0342|Sensor de fase (comando) – sinal baixo
P0343|Sensor de fase (comando) – sinal alto
P0344|Sensor de fase (comando) – intermitente
P0354|Bobina de ignição do cilindro 4 – circuito
P0365|Sensor de fase (escapamento) – circuito
P0400|Fluxo de recirculação (EGR) – falha
P0401|EGR – fluxo insuficiente
P0402|EGR – fluxo excessivo
P0403|Válvula EGR – circuito
P0404|Válvula EGR – faixa de operação
P0405|Sensor EGR – sinal baixo
P0406|Sensor EGR – sinal alto
P0410|Sistema de ar secundário – falha
P0411|Ar secundário – fluxo incorreto
P0412|Válvula de ar secundário – circuito
P0430|Eficiência do catalisador banco 2 abaixo do limite
P0446|EVAP – circuito de ventilação com defeito
P0447|EVAP – válvula de ventilação aberta
P0448|EVAP – válvula de ventilação em curto
P0449|EVAP – válvula de ventilação – circuito
P0450|Sensor de pressão do EVAP – circuito
P0451|Sensor de pressão do EVAP – faixa
P0452|Sensor de pressão do EVAP – sinal baixo
P0453|Sensor de pressão do EVAP – sinal alto
P0457|EVAP – vazamento (tampa do tanque solta ou ausente)
P0461|Sensor de nível de combustível – faixa
P0462|Sensor de nível de combustível – sinal baixo
P0463|Sensor de nível de combustível – sinal alto
P0464|Sensor de nível de combustível – intermitente
P0481|Ventilador de arrefecimento 2 – circuito
P0482|Ventilador de arrefecimento 3 – circuito
P0483|Ventilador de arrefecimento – teste de desempenho
P0485|Ventilador de arrefecimento – alimentação
P0502|Sensor de velocidade do veículo – sinal baixo
P0503|Sensor de velocidade do veículo – intermitente
P0508|Controle de marcha lenta – sinal baixo
P0509|Controle de marcha lenta – sinal alto
P0510|Interruptor do pedal do acelerador (marcha lenta) – falha
P0520|Sensor de pressão do óleo – circuito
P0521|Sensor de pressão do óleo – desempenho
P0522|Sensor de pressão do óleo – sinal baixo
P0523|Sensor de pressão do óleo – sinal alto
P0524|Pressão do óleo do motor muito baixa
P0530|Sensor de pressão do ar-condicionado – circuito
P0532|Sensor de pressão do ar-condicionado – sinal baixo
P0533|Sensor de pressão do ar-condicionado – sinal alto
P0550|Sensor de pressão da direção hidráulica – circuito
P0551|Sensor de pressão da direção hidráulica – faixa
P0571|Interruptor do freio (controle de velocidade) – circuito
P0572|Interruptor do freio – sinal baixo
P0573|Interruptor do freio – sinal alto
P0575|Controle de velocidade – circuito de entrada
P0603|Memória de reserva (KAM) da ECU – erro
P0604|Memória RAM da ECU – erro
P0605|Memória ROM da ECU – erro
P0607|Desempenho do módulo de controle
P0610|Opções do veículo (ECU) – incompatibilidade
P0615|Relé do motor de partida – circuito
P0616|Relé do motor de partida – sinal baixo
P0617|Relé do motor de partida – sinal alto
P0621|Lâmpada do alternador – circuito
P0622|Campo do alternador – circuito
P0628|Bomba de combustível – sinal baixo
P0629|Bomba de combustível – sinal alto
P0642|Tensão de referência do sensor A – baixa
P0643|Tensão de referência do sensor A – alta
P0652|Tensão de referência do sensor B – baixa
P0653|Tensão de referência do sensor B – alta
P0660|Atuador do coletor de admissão – circuito
P0670|Módulo de velas aquecedoras – circuito
P0686|Relé principal da ECU – sinal baixo
P0687|Relé principal da ECU – sinal alto
P0688|Relé principal da ECU – corrente alta
P0691|Ventilador de arrefecimento 1 – sinal baixo
P0692|Ventilador de arrefecimento 1 – sinal alto
P0705|Sensor de posição do câmbio – circuito
P0715|Sensor de velocidade de entrada do câmbio – circuito
P0720|Sensor de velocidade de saída do câmbio – circuito
P0730|Relação de marcha incorreta
P0741|Conversor de torque (embreagem) – desempenho
P0750|Solenoide de câmbio A – circuito
P2096|Pós-catalisador – mistura pobre B1
P2097|Pós-catalisador – mistura rica B1
P2102|Motor da borboleta eletrônica – sinal baixo
P2103|Motor da borboleta eletrônica – sinal alto
P2104|Controle da borboleta – modo de segurança (limitado)
P2105|Controle da borboleta – desligamento forçado
P2106|Controle da borboleta – potência limitada
P2107|Controle da borboleta – módulo com defeito
P2110|Controle da borboleta – RPM limitada
P2111|Borboleta travada aberta
P2112|Borboleta travada fechada
P2122|Pedal do acelerador D – sinal baixo
P2123|Pedal do acelerador D – sinal alto
P2127|Pedal do acelerador E – sinal baixo
P2128|Pedal do acelerador E – sinal alto
P2136|Sensores do acelerador A/C – correlação
P2176|Borboleta – posição de repouso não aprendida
P2177|Mistura pobre fora da marcha lenta B1
P2178|Mistura rica fora da marcha lenta B1
P2229|Sensor de pressão barométrica – sinal alto
P2237|Sonda larga B1S1 – circuito
P2270|Sonda lambda B1S2 – travada em pobre
P2271|Sonda lambda B1S2 – travada em rica
P2610|Temporizador da ECU – falha
U0073|Barramento de comunicação CAN – desligado
U0101|Sem comunicação com o módulo do câmbio
U0122|Sem comunicação com o controle de estabilidade
U0131|Sem comunicação com a direção elétrica
U0164|Sem comunicação com o ar-condicionado
U0401|Dados inválidos recebidos da ECU
U0415|Dados inválidos recebidos do ABS
"""

    private val extra: Map<String, String> by lazy {
        extraRaw.lines().map { it.trim() }.filter { it.contains('|') }
            .associate { it.substringBefore('|') to it.substringAfter('|') }
    }

    /** Gravidade aproximada para dar cor ao código. 2 = pare/risco, 1 = atenção, 0 = informativo. */
    fun severity(code: String): Int = when {
        code in setOf("P0217", "P0524", "P0300", "P0301", "P0302", "P0303", "P0304", "P0016", "P0017") -> 2
        code.startsWith("P03") && code.length == 5 && code[2] == '0' && code[3] == '0' -> 2
        code.startsWith("U") -> 1
        code in setOf("P0442", "P0455", "P0456", "P0457", "P0440", "P0128", "P0125") -> 0
        else -> 1
    }

    fun describe(code: String): String {
        table[code]?.let { return it }
        extra[code]?.let { return it }
        return when (code.firstOrNull()) {
            'P' -> if (code.getOrNull(1) == '0')
                "Código genérico de motor/câmbio (sem descrição local — pesquise \"$code\")"
            else "Código específico do fabricante (Fiat) — pesquise \"$code\" Argo"
            'U' -> "Falha de comunicação entre módulos (rede CAN)"
            'B' -> "Falha na carroceria/conforto"
            'C' -> "Falha no chassi (freios/suspensão)"
            else -> "Sem descrição"
        }
    }
}
