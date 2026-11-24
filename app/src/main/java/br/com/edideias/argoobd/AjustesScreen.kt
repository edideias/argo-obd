package br.com.edideias.argoobd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("MissingPermission")
private fun bonded(ctx: Context): List<Pair<String, String>> = try {
    val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bm.adapter?.bondedDevices?.map { (it.name ?: "sem nome") to it.address }?.sortedBy { it.first } ?: emptyList()
} catch (e: Exception) {
    emptyList()
}

@Composable
private fun Num(label: String, value: String, set: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = set, singleLine = true,
        label = { T(label, 12.sp, C.DIM) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = fieldColors(), modifier = modifier
    )
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(title, 14.sp, C.TEXT, FontWeight.Medium)
            if (sub.isNotEmpty()) T(sub, 11.sp, C.DIM)
        }
        Switch(
            checked = on, onCheckedChange = set,
            colors = SwitchDefaults.colors(checkedTrackColor = C.TEAL, checkedThumbColor = Color.White, uncheckedTrackColor = C.TRACK, uncheckedThumbColor = C.DIM, uncheckedBorderColor = C.LINE)
        )
    }
}

@Composable
fun AjustesScreen(m: Manager, onReconnect: () -> Unit, hasPerm: Boolean, askPerm: () -> Unit) {
    val ctx = LocalContext.current
    val cfg = m.cfg
    var devices by remember { mutableStateOf(bonded(ctx)) }
    var chosen by remember { mutableStateOf(cfg.lastDevice) }
    var demo by remember { mutableStateOf(cfg.demo) }
    var keep by remember { mutableStateOf(cfg.keepOn) }
    var alerts by remember { mutableStateOf(cfg.alertsOn) }
    var fuel by remember { mutableStateOf(cfg.fuelType) }
    var eth by remember { mutableStateOf(cfg.customEth.toString()) }
    var pg by remember { mutableStateOf(Fmt.num(cfg.priceGas.toDouble(), 2)) }
    var pe by remember { mutableStateOf(Fmt.num(cfg.priceEth.toDouble(), 2)) }
    var disp by remember { mutableStateOf(Fmt.num(cfg.disp.toDouble(), 3)) }
    var ve by remember { mutableStateOf(Fmt.num(cfg.ve.toDouble(), 2)) }
    var tank by remember { mutableStateOf(Fmt.num(cfg.tank.toDouble(), 0)) }
    var odo by remember { mutableStateOf(Fmt.num(cfg.odo.toDouble(), 0)) }
    var tmax by remember { mutableStateOf(cfg.tempMax.toString()) }
    var vmin by remember { mutableStateOf(Fmt.num(cfg.voltMin.toDouble(), 1)) }
    var rmax by remember { mutableStateOf(cfg.rpmMax.toString()) }
    var smax by remember { mutableStateOf(cfg.speedMax.toString()) }
    var msg by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Section("Leitor Bluetooth")
        Panel {
            if (!hasPerm) {
                T("O app precisa da permissão de Bluetooth para ver os aparelhos pareados.", 13.sp, C.WARN)
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Permitir Bluetooth") { askPerm() }
            } else {
                T("Pareie o leitor antes nas configurações do Android (senha 1234 ou 0000) e escolha aqui.", 12.sp, C.DIM)
                Spacer(Modifier.height(8.dp))
                if (devices.isEmpty()) T("Nenhum aparelho pareado.", 13.sp, C.DIM)
                for ((n, a) in devices) {
                    Chip("$n  ·  $a", chosen == a) {
                        chosen = a
                        cfg.lastDevice = a
                        cfg.lastDeviceName = n
                    }
                    Spacer(Modifier.height(6.dp))
                }
                GhostButton("Atualizar lista") { devices = bonded(ctx) }
            }
        }
        Toggle("Modo demonstração", "Simula um carro, sem leitor. Bom para conhecer o app.", demo) {
            demo = it; cfg.demo = it
        }
        PrimaryButton(if (m.isRunning()) "Reconectar" else "Conectar", Modifier.fillMaxWidth()) { onReconnect() }
        if (m.isRunning()) GhostButton("Desconectar", Modifier.fillMaxWidth(), color = C.BAD) { m.stop() }

        Section("Combustível e preços")
        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Gasolina (E30)", fuel == 0) { fuel = 0 }
                Chip("Etanol", fuel == 1) { fuel = 1 }
                Chip("Mistura", fuel == 2) { fuel = 2 }
            }
            if (fuel == 2) {
                Spacer(Modifier.height(8.dp))
                Num("% de etanol no tanque", eth, { eth = it }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Gasolina R$/L", pg, { pg = it }, Modifier.weight(1f))
                Num("Etanol R$/L", pe, { pe = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Num("Tanque (litros)", tank, { tank = it }, Modifier.fillMaxWidth())
        }

        Section("Motor (para estimar consumo)")
        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Cilindrada (L)", disp, { disp = it }, Modifier.weight(1f))
                Num("Eficiência vol.", ve, { ve = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            T("Só é usado quando a ECU não informa o consumo nem o fluxo de ar. Padrão do Argo 1.0: 0,999 e 0,85.", 11.sp, C.DIM)
        }

        Section("Alertas")
        Panel {
            Toggle("Alertas no Painel", "Avisa no topo quando algo passar do limite.", alerts) { alerts = it; cfg.alertsOn = it }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Água acima de °C", tmax, { tmax = it }, Modifier.weight(1f))
                Num("Bateria abaixo de V", vmin, { vmin = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Giro acima de (0 = off)", rmax, { rmax = it }, Modifier.weight(1f))
                Num("Velocidade (0 = off)", smax, { smax = it }, Modifier.weight(1f))
            }
        }

        Section("Hodômetro e tela")
        Panel {
            Num("Km do carro agora (para a manutenção)", odo, { odo = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Toggle("Manter a tela ligada", "Recomendado enquanto o app está no suporte.", keep) { keep = it; cfg.keepOn = it }
        }

        PrimaryButton("Salvar ajustes", Modifier.fillMaxWidth()) {
            cfg.fuelType = fuel
            cfg.customEth = (parseNum(eth) ?: 50.0).toInt().coerceIn(0, 100)
            parseNum(pg)?.let { cfg.priceGas = it.toFloat() }
            parseNum(pe)?.let { cfg.priceEth = it.toFloat() }
            parseNum(disp)?.let { cfg.disp = it.toFloat().coerceIn(0.3f, 6f) }
            parseNum(ve)?.let { cfg.ve = it.toFloat().coerceIn(0.4f, 1.2f) }
            parseNum(tank)?.let { cfg.tank = it.toFloat().coerceIn(10f, 120f) }
            parseNum(odo)?.let { cfg.odo = it.toFloat().coerceAtLeast(0f) }
            parseNum(tmax)?.let { cfg.tempMax = it.toInt() }
            parseNum(vmin)?.let { cfg.voltMin = it.toFloat() }
            parseNum(rmax)?.let { cfg.rpmMax = it.toInt() }
            parseNum(smax)?.let { cfg.speedMax = it.toInt() }
            msg = "Ajustes salvos."
        }
        if (msg.isNotEmpty()) T(msg, 13.sp, C.TEAL)
        T("Argo OBD 2.0 · feito para o ELM327 Mini Bluetooth", 11.sp, C.FAINT, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}
