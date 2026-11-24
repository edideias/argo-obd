package br.com.edideias.argoobd

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class FalhasPage(private val a: MainActivity) : Page {
    private val m get() = a.mgr
    private val scroll = ScrollView(a)
    override val view: View get() = scroll
    private val col = a.vbox().apply { setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(20)) }
    private val status = a.tv("", 13f, C.DIM)
    private var fixedCount = 0

    init {
        col.addView(status, lp(MATCH, WRAP, b = a.dp(8)))
        val r = a.hbox()
        r.addView(a.btn("Ler falhas") { read() }, lp(0, WRAP, 1f, r = a.dp(5)))
        r.addView(a.btn("Apagar falhas", false) { confirmClear() }, lp(0, WRAP, 1f, l = a.dp(5)))
        col.addView(r, lp(MATCH, WRAP, b = a.dp(6)))
        fixedCount = col.childCount
        col.addView(a.tv("Toque em \"Ler falhas\" para consultar a ECU.", 13f, C.DIM).apply {
            setPadding(a.dp(4), a.dp(10), 0, 0)
        })
        scroll.addView(col)
    }

    private fun clear() {
        while (col.childCount > fixedCount) col.removeViewAt(col.childCount - 1)
    }

    private fun toast(s: String) = Toast.makeText(a, s, Toast.LENGTH_LONG).show()

    private fun section(title: String, list: List<String>?, emptyText: String) {
        col.addView(a.sectionTitle(title))
        if (list == null) {
            col.addView(a.tv("A ECU não respondeu a este tipo de consulta.", 13f, C.DIM).apply { setPadding(a.dp(4), 0, 0, 0) })
            return
        }
        if (list.isEmpty()) {
            val c = a.card(12)
            c.addView(a.tv(emptyText, 14f, C.GOOD, true))
            col.addView(c)
            return
        }
        for (code in list) {
            val c = a.card(12)
            c.addView(a.tv(code, 24f, C.BAD, true))
            c.addView(a.tv(Dtc.describe(code), 14f, C.TEXT).apply { setPadding(0, a.dp(4), 0, 0) })
            col.addView(c, lp(MATCH, WRAP, b = a.dp(8)))
        }
    }

    private fun read() {
        if (!m.connected) {
            toast("Conecte ao leitor primeiro (aba AJUSTES)")
            return
        }
        status.text = "Consultando a ECU…"
        m.readDtcs { r ->
            clear()
            val d = r.getOrNull()
            if (d == null) {
                status.text = r.exceptionOrNull()?.message ?: "Sem resposta"
                return@readDtcs
            }
            status.text = "Leitura concluída"
            section("Falhas confirmadas", d.stored, "Nenhuma falha armazenada")
            section("Falhas pendentes", d.pending, "Nenhuma falha pendente")
            section("Falhas permanentes", d.permanent, "Nenhuma falha permanente")
        }
    }

    private fun confirmClear() {
        if (!m.connected) {
            toast("Conecte ao leitor primeiro")
            return
        }
        AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Apagar falhas?")
            .setMessage("Isso apaga os códigos e os dados congelados e zera os testes de emissões. A luz de injeção apaga, mas volta se o defeito continuar. Use só depois de entender o problema.")
            .setPositiveButton("Apagar") { _, _ ->
                m.clearDtcs { r ->
                    val ok = r.getOrNull() == true
                    toast(if (ok) "Falhas apagadas" else "A ECU não aceitou apagar (ligue a ignição / motor desligado)")
                    if (ok) read()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun refresh() {
        val off = "Sem conexão com o leitor."
        if (!m.connected) {
            if (status.text.toString() != off) status.text = off
        } else if (status.text.toString() == off || status.text.isEmpty()) {
            status.text = if (m.milOn) "Luz de injeção acesa — ${m.dtcCount} falha(s)" else "Conectado"
        }
    }
}
