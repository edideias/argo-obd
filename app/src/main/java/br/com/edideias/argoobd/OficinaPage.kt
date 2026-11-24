package br.com.edideias.argoobd

import android.app.AlertDialog
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class OficinaPage(private val a: MainActivity) : Page {
    private val m get() = a.mgr
    private val root = a.vbox()
    override val view: View get() = root

    private val body = android.widget.FrameLayout(a)
    private val subTabs = ArrayList<TextView>()
    private var sub = 0

    // ---- ao vivo ----
    private val liveScroll = ScrollView(a)
    private val liveCol = a.vbox().apply { setPadding(a.dp(14), a.dp(4), a.dp(14), a.dp(20)) }
    private val liveHead = a.tv("", 12f, C.DIM)
    private val logBtn: TextView
    private val rows = LinkedHashMap<Int, Pair<TextView, TextView>>()
    private var builtFor: List<Int> = emptyList()

    // ---- prontidão ----
    private val readyScroll = ScrollView(a)
    private val readyCol = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(20)) }

    // ---- congelados ----
    private val freezeScroll = ScrollView(a)
    private val freezeCol = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(20)) }

    // ---- terminal ----
    private val termBox = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(8)) }
    private val termOut = a.tv("Terminal do leitor. Toque em um comando ou digite o seu.\n", 12f, C.GOOD).apply {
        typeface = Typeface.MONOSPACE
        setTextIsSelectable(true)
    }
    private val termScroll = ScrollView(a)
    private val termIn = EditText(a)

    // ---- info ----
    private val infoScroll = ScrollView(a)
    private val infoCol = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(20)) }

    init {
        val names = listOf("Ao vivo", "Prontidão", "Congelados", "Terminal", "Info")
        val bar = a.hbox().apply { setPadding(a.dp(10), a.dp(6), a.dp(10), a.dp(6)) }
        names.forEachIndexed { i, n ->
            val t = a.tv(n, 13f, C.DIM, true).apply {
                gravity = Gravity.CENTER
                setPadding(a.dp(12), a.dp(8), a.dp(12), a.dp(8))
                setOnClickListener { selectSub(i) }
            }
            subTabs.add(t)
            bar.addView(t, lp(WRAP, WRAP, r = a.dp(6)))
        }
        val hs = HorizontalScrollView(a).apply {
            isHorizontalScrollBarEnabled = false
            addView(bar)
        }
        root.addView(hs, lp(MATCH, WRAP))
        root.addView(body, lp(MATCH, 0, 1f))

        // ao vivo
        val top = a.hbox()
        logBtn = a.btn("Gravar CSV", false) { toggleLog() }
        top.addView(logBtn, lp(0, WRAP, 1f, r = a.dp(5)))
        top.addView(a.btn("Zerar mín/máx", false) { m.resetMinMax() }, lp(0, WRAP, 1f, l = a.dp(5)))
        liveCol.addView(top, lp(MATCH, WRAP, b = a.dp(8)))
        liveHead.setPadding(a.dp(2), 0, 0, a.dp(6))
        liveCol.addView(liveHead)
        liveScroll.addView(liveCol)

        // prontidão
        readyScroll.addView(readyCol)
        readyCol.addView(a.btn("Ler prontidão (monitores de emissões)") { loadReadiness() })
        readyCol.addView(a.tv("Mostra se o carro já completou os autotestes de emissões — útil antes de inspeção veicular.", 12f, C.DIM).apply {
            setPadding(a.dp(2), a.dp(8), 0, a.dp(8))
        })

        // congelados
        freezeScroll.addView(freezeCol)
        freezeCol.addView(a.btn("Ler dados congelados") { loadFreeze() })
        freezeCol.addView(a.tv("Foto dos sensores no instante em que a última falha foi registrada.", 12f, C.DIM).apply {
            setPadding(a.dp(2), a.dp(8), 0, a.dp(8))
        })

        // terminal
        termScroll.addView(termOut)
        val chips = a.hbox()
        for (c in listOf("ATZ", "ATI", "ATDP", "ATRV", "0100", "0101", "03", "0902")) {
            chips.addView(a.btn(c, false) { sendTerm(c) }.apply { textSize = 12f }, lp(WRAP, WRAP, r = a.dp(6)))
        }
        val chipsH = HorizontalScrollView(a).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        }
        termBox.addView(chipsH, lp(MATCH, WRAP, b = a.dp(8)))
        termBox.addView(termScroll, lp(MATCH, 0, 1f))
        termIn.hint = "comando (ex.: 010C)"
        termIn.setHintTextColor(C.DIM)
        termIn.setTextColor(C.TEXT)
        termIn.typeface = Typeface.MONOSPACE
        termIn.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        termIn.imeOptions = EditorInfo.IME_ACTION_SEND
        termIn.setSingleLine(true)
        termIn.setOnEditorActionListener { _, _, _ ->
            val c = termIn.text.toString().trim()
            if (c.isNotEmpty()) sendTerm(c)
            termIn.setText("")
            true
        }
        val send = a.hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        send.addView(termIn, lp(0, WRAP, 1f))
        send.addView(a.btn("Enviar") {
            val c = termIn.text.toString().trim()
            if (c.isNotEmpty()) sendTerm(c)
            termIn.setText("")
        }, lp(WRAP, WRAP, l = a.dp(8)))
        termBox.addView(send, lp(MATCH, WRAP, t = a.dp(8)))

        // info
        infoScroll.addView(infoCol)
        infoCol.addView(a.btn("Ler informações da ECU") { loadInfo() })

        selectSub(0)
    }

    private fun selectSub(i: Int) {
        sub = i
        subTabs.forEachIndexed { idx, t ->
            t.setTextColor(if (idx == i) C.BG else C.DIM)
            t.background = roundBg(if (idx == i) C.ACCENT else C.CARD, a.dpf(18f))
        }
        body.removeAllViews()
        body.addView(
            when (i) {
                0 -> liveScroll
                1 -> readyScroll
                2 -> freezeScroll
                3 -> termBox
                else -> infoScroll
            }
        )
    }

    private fun toast(s: String) = Toast.makeText(a, s, Toast.LENGTH_LONG).show()

    private fun toggleLog() {
        if (m.logging) {
            toast(m.stopLog())
        } else {
            if (!m.connected) {
                toast("Conecte primeiro")
                return
            }
            if (!m.startLog()) toast("Não consegui iniciar a gravação")
        }
    }

    private fun addCard(col: LinearLayout, title: String, text: String, color: Int = C.TEXT) {
        val c = a.card(12)
        c.addView(a.tv(title, 12f, C.DIM))
        c.addView(a.tv(text, 15f, color, true).apply { setTextIsSelectable(true) })
        col.addView(c, lp(MATCH, WRAP, t = a.dp(8)))
    }

    private fun clearBelow(col: LinearLayout, keep: Int) {
        while (col.childCount > keep) col.removeViewAt(col.childCount - 1)
    }

    private fun loadReadiness() {
        m.readReadiness { r ->
            clearBelow(readyCol, 2)
            val b = r.getOrNull()
            if (b == null || b.size < 4) {
                addCard(readyCol, "Prontidão", "A ECU não respondeu", C.WARN)
                return@readReadiness
            }
            val mil = (b[0] and 0x80) != 0
            addCard(readyCol, "Luz de injeção (MIL)", if (mil) "ACESA — ${b[0] and 0x7F} falha(s)" else "Apagada", if (mil) C.BAD else C.GOOD)
            val items = listOf(
                Triple("Falha de ignição", 0, 4),
                Triple("Sistema de combustível", 1, 5),
                Triple("Componentes", 2, 6)
            )
            for ((n, bitA, bitI) in items) {
                val avail = (b[1] shr bitA) and 1 == 1
                val incomplete = (b[1] shr bitI) and 1 == 1
                addCard(readyCol, "$n (contínuo)", if (!avail) "não disponível" else if (incomplete) "incompleto" else "completo", if (avail && incomplete) C.WARN else C.TEXT)
            }
            val names = listOf("Catalisador", "Catalisador aquecido", "Sistema evaporativo", "Ar secundário", "Ar-condicionado", "Sonda lambda", "Aquecedor da sonda", "EGR")
            for (i in 0..7) {
                val avail = (b[2] shr i) and 1 == 1
                val incomplete = (b[3] shr i) and 1 == 1
                if (!avail) continue
                addCard(readyCol, names[i], if (incomplete) "incompleto (ainda não testou)" else "completo", if (incomplete) C.WARN else C.GOOD)
            }
        }
    }

    private fun loadFreeze() {
        m.readFreeze { r ->
            clearBelow(freezeCol, 2)
            val list = r.getOrNull()
            if (list == null) {
                addCard(freezeCol, "Dados congelados", r.exceptionOrNull()?.message ?: "Sem resposta", C.WARN)
                return@readFreeze
            }
            if (list.size <= 1 && list[0].second == "nenhuma") {
                addCard(freezeCol, "Dados congelados", "Nenhum registro — não há falha armazenada", C.GOOD)
                return@readFreeze
            }
            for ((k, v) in list) addCard(freezeCol, k, v)
        }
    }

    private fun loadInfo() {
        m.readInfo { r ->
            clearBelow(infoCol, 1)
            val list = r.getOrNull()
            if (list == null) {
                addCard(infoCol, "Informações", r.exceptionOrNull()?.message ?: "Sem resposta", C.WARN)
                return@readInfo
            }
            for ((k, v) in list) addCard(infoCol, k, v)
        }
    }

    private fun sendTerm(cmd: String) {
        termOut.append("> $cmd\n")
        m.terminal(cmd) { r ->
            val s = r.getOrNull()
            termOut.append((s ?: ("erro: " + (r.exceptionOrNull()?.message ?: "?"))) + "\n\n")
            termScroll.post { termScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun showChart(pid: Int) {
        val d = a.vbox().apply { setPadding(a.dp(16), a.dp(12), a.dp(16), a.dp(4)) }
        val sp = Spark(a).apply { data = m.history(pid); unit = Pids.unitOf(pid) }
        d.addView(sp, lp(MATCH, a.dp(180)))
        val mn = m.mins[pid]
        val mx = m.maxs[pid]
        d.addView(a.tv("Mín ${Fmt.pid(pid, mn)}   •   Máx ${Fmt.pid(pid, mx)}   •   ${m.history(pid).size} amostras", 12f, C.DIM).apply {
            setPadding(0, a.dp(8), 0, 0)
        })
        AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(Pids.nameOf(pid))
            .setView(d)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun rebuild(list: List<Int>) {
        while (liveCol.childCount > 2) liveCol.removeViewAt(liveCol.childCount - 1)
        rows.clear()
        for (pid in list) {
            val r = a.hbox().apply {
                background = roundBg(C.CARD, a.dpf(12f))
                setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10))
                gravity = Gravity.CENTER_VERTICAL
                setOnClickListener { showChart(pid) }
            }
            val left = a.vbox()
            left.addView(a.tv(Pids.nameOf(pid), 14f, C.TEXT))
            val mm = a.tv("", 11f, C.DIM)
            left.addView(mm)
            val vv = a.tv("—", 18f, C.ACCENT, true).apply { gravity = Gravity.END }
            r.addView(left, lp(0, WRAP, 1f))
            r.addView(vv, lp(WRAP, WRAP))
            liveCol.addView(r, lp(MATCH, WRAP, t = a.dp(6)))
            rows[pid] = Pair(vv, mm)
        }
        builtFor = list
    }

    override fun refresh() {
        if (sub != 0) return
        logBtn.text = if (m.logging) "Parar e salvar CSV" else "Gravar CSV"
        val list = if (m.connected) (m.supported + Pids.VOLT).distinct() else emptyList()
        if (list != builtFor) rebuild(list)
        liveHead.text = when {
            !m.connected -> "Sem conexão. Conecte em AJUSTES (ou use o modo demonstração)."
            else -> "${m.status} • ${m.supported.size} sensores aceitos pela ECU • toque em um para ver o gráfico"
        }
        for ((pid, p) in rows) {
            val v = m.fresh(pid, 15000)
            p.first.text = Fmt.pid(pid, v) + " " + Pids.unitOf(pid)
            p.first.setTextColor(if (v == null) C.DIM else C.ACCENT)
            val mn = m.mins[pid]
            val mx = m.maxs[pid]
            p.second.text = if (mn != null && mx != null)
                "PID %02X  •  mín %s  máx %s".format(pid and 0xFF, Fmt.pid(pid, mn), Fmt.pid(pid, mx))
            else "PID %02X".format(pid and 0xFF)
        }
    }
}
