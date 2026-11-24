package br.com.edideias.argoobd

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun share(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(i, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun google(ctx: Context, code: String) {
    val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode("$code Fiat Argo 1.0 Firefly")))
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(i) } catch (_: Exception) {}
}

@Composable
fun FalhasScreen(m: Manager) {
    val ctx = LocalContext.current
    var res by remember { mutableStateOf<DtcResult?>(null) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }

    fun read() {
        busy = true; err = ""; info = ""
        m.readDtcs { r ->
            busy = false
            r.onSuccess { res = it; if (it.stored == null && it.pending == null && it.permanent == null) err = "A ECU não respondeu. Ligue a ignição e tente de novo." }
            r.onFailure { err = it.message ?: "Erro" }
        }
    }

    Live { _ ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Panel(border = if (m.milOn) C.BAD.copy(alpha = 0.6f) else C.LINE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(if (m.milOn) C.BAD else C.GOOD, 12.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        T(if (m.milOn) "Luz de injeção acesa" else "Luz de injeção apagada", 16.sp, C.TEXT, FontWeight.Bold)
                        T(if (m.connected) "${m.dtcCount} falha(s) confirmada(s) na ECU" else "Conecte ao carro para ler", 12.sp, C.DIM)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(if (busy) "Lendo…" else "Ler falhas", enabled = !busy && m.connected) { read() }
                GhostButton("Apagar falhas", color = C.BAD) { if (m.connected) confirm = true }
            }
            if (err.isNotEmpty()) T(err, 13.sp, C.BAD)
            if (info.isNotEmpty()) T(info, 13.sp, C.TEAL)
            val r = res
            if (r != null) {
                val all = ArrayList<String>()
                fun block(title: String, hint: String, list: List<String>?) {
                    if (list == null) return
                    all.addAll(list)
                }
                Section("Confirmadas")
                CodeList(ctx, r.stored, "Nenhuma falha confirmada.")
                Section("Pendentes (apareceram, ainda não confirmadas)")
                CodeList(ctx, r.pending, "Nenhuma pendente.")
                Section("Permanentes")
                CodeList(ctx, r.permanent, "Nenhuma permanente.")
                val text = buildString {
                    append("Argo OBD — falhas do Fiat Argo\n")
                    for ((t, l) in listOf("Confirmadas" to r.stored, "Pendentes" to r.pending, "Permanentes" to r.permanent)) {
                        append("\n$t:\n")
                        if (l.isNullOrEmpty()) append("  nenhuma\n") else for (c in l) append("  $c — ${Dtc.describe(c)}\n")
                    }
                }
                GhostButton("Compartilhar relatório") { share(ctx, text) }
            }
            T("Apagar as falhas também zera os monitores de prontidão e o dado congelado. Se o defeito continuar, o código volta.", 11.sp, C.DIM)
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = C.CARD1,
            title = { T("Apagar as falhas?", 17.sp, C.TEXT, FontWeight.Bold) },
            text = { T("Isso apaga os códigos, o dado congelado e zera a prontidão. Anote ou compartilhe antes se quiser guardar.", 13.sp, C.DIM) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    m.clearDtcs { r ->
                        info = if (r.getOrNull() == true) "Falhas apagadas." else "A ECU não confirmou. Ignição ligada e motor desligado?"
                        read()
                    }
                }) { T("Apagar", 14.sp, C.BAD, FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { T("Cancelar", 14.sp, C.DIM) } }
        )
    }
}

@Composable
private fun CodeList(ctx: Context, list: List<String>?, emptyText: String) {
    if (list == null) {
        Panel { T("Sem resposta da ECU para esta consulta.", 13.sp, C.DIM) }
        return
    }
    if (list.isEmpty()) {
        Panel { Row(verticalAlignment = Alignment.CenterVertically) { Dot(C.GOOD); Spacer(Modifier.width(8.dp)); T(emptyText, 13.sp, C.GOOD) } }
        return
    }
    for (c in list) {
        val sev = Dtc.severity(c)
        val col = when (sev) { 2 -> C.BAD; 1 -> C.WARN; else -> C.SKY }
        Panel(pad = 12.dp, border = col.copy(alpha = 0.45f), onClick = { google(ctx, c) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(c, 20.sp, col, FontWeight.Bold, Modifier.width(86.dp))
                Column(Modifier.weight(1f)) {
                    T(Dtc.describe(c), 13.sp, C.TEXT)
                    T("toque para pesquisar", 10.sp, C.DIM)
                }
                Pill(when (sev) { 2 -> "grave"; 1 -> "atenção"; else -> "leve" }, col)
            }
        }
    }
}
