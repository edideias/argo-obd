package br.com.edideias.argoobd

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Theme(
    val name: String, val desc: String,
    val bg: Color, val bg2: Color, val card1: Color, val card2: Color, val line: Color,
    val text: Color, val dim: Color, val faint: Color,
    val a1: Color, val a2: Color, val good: Color, val warn: Color, val bad: Color, val cold: Color, val track: Color,
    val gauge: Int, // 0 arco, 1 digital segmentado, 2 analógico
    val cut: Boolean, val mono: Boolean
)

object Themes {
    val list = listOf(
        Theme(
            "Carbono", "Escuro e elegante",
            Color(0xFF05080D), Color(0xFF0A111A), Color(0xFF141D29), Color(0xFF0E151F), Color(0x1FFFFFFF),
            Color(0xFFEAF1F8), Color(0xFF8493A6), Color(0xFF4A5868),
            Color(0xFF2DD4BF), Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFFF87171), Color(0xFF60A5FA), Color(0xFF18222E),
            0, false, false
        ),
        Theme(
            "Hi-Tech", "Digital, neon e futurista",
            Color(0xFF01040B), Color(0xFF031225), Color(0xFF071B30), Color(0xFF030D1B), Color(0x4D00E5FF),
            Color(0xFFD6F7FF), Color(0xFF6FA8BD), Color(0xFF2B5870),
            Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFF00FF9C), Color(0xFFFFD400), Color(0xFFFF2E63), Color(0xFF3DA5FF), Color(0xFF0A2236),
            1, true, true
        ),
        Theme(
            "Clássico", "Painel analógico com ponteiro",
            Color(0xFF0A0908), Color(0xFF15110D), Color(0xFF211B15), Color(0xFF16120E), Color(0x2EFFE2B8),
            Color(0xFFF6EEE3), Color(0xFFA89886), Color(0xFF574B3F),
            Color(0xFFFF9F1C), Color(0xFFFFD166), Color(0xFF9BD34A), Color(0xFFFFB703), Color(0xFFE63946), Color(0xFF6FA8DC), Color(0xFF2B241D),
            2, false, false
        )
    )
    var id by mutableStateOf(0)
    val cur: Theme get() = list[id.coerceIn(0, list.size - 1)]
}

object C {
    val BG: Color get() = Themes.cur.bg
    val BG2: Color get() = Themes.cur.bg2
    val CARD1: Color get() = Themes.cur.card1
    val CARD2: Color get() = Themes.cur.card2
    val LINE: Color get() = Themes.cur.line
    val TEXT: Color get() = Themes.cur.text
    val DIM: Color get() = Themes.cur.dim
    val FAINT: Color get() = Themes.cur.faint
    val TEAL: Color get() = Themes.cur.a1
    val SKY: Color get() = Themes.cur.a2
    val GOOD: Color get() = Themes.cur.good
    val WARN: Color get() = Themes.cur.warn
    val BAD: Color get() = Themes.cur.bad
    val COLD: Color get() = Themes.cur.cold
    val TRACK: Color get() = Themes.cur.track
}

val Brand: Brush get() = Brush.horizontalGradient(listOf(C.TEAL, C.SKY))

fun panelShape(): Shape =
    if (Themes.cur.cut) CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp) else RoundedCornerShape(22.dp)

fun buttonShape(): Shape =
    if (Themes.cur.cut) CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(16.dp)

/** Recompõe a tela a cada 250 ms para ler os dados ao vivo. */
@Composable
fun Live(content: @Composable (Int) -> Unit) {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            tick++
        }
    }
    content(tick)
}

@Composable
fun T(
    text: String,
    size: TextUnit = 14.sp,
    color: Color = C.TEXT,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    mono: Boolean = false,
    lines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        fontSize = size,
        color = color,
        fontWeight = weight,
        modifier = modifier,
        textAlign = align,
        maxLines = lines,
        fontFamily = if (mono || (Themes.cur.mono && size.value >= 19f)) FontFamily.Monospace else FontFamily.SansSerif
    )
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    pad: Dp = 14.dp,
    border: Color = C.LINE,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = panelShape()
    var m = modifier
        .clip(shape)
        .background(Brush.verticalGradient(listOf(C.CARD1, C.CARD2)))
        .border(1.dp, border, shape)
    if (onClick != null) m = m.clickable { onClick() }
    Column(modifier = m.padding(pad), content = content)
}

@Composable
fun Label(text: String) {
    T(text.uppercase(), 10.sp, C.DIM, FontWeight.SemiBold)
}

@Composable
fun Section(text: String, modifier: Modifier = Modifier) {
    T(text, 13.sp, C.TEAL, FontWeight.Bold, modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { T(text, 11.sp, color, FontWeight.SemiBold) }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = buttonShape()
    Box(
        modifier
            .clip(shape)
            .background(if (enabled) Brand else Brush.horizontalGradient(listOf(C.TRACK, C.TRACK)))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) { T(text, 14.sp, if (enabled) Color(0xFF031418) else C.DIM, FontWeight.Bold) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = C.TEXT, onClick: () -> Unit) {
    val shape = buttonShape()
    Box(
        modifier
            .clip(shape)
            .border(1.dp, C.LINE, shape)
            .background(Color(0x0DFFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { T(text, 13.sp, color, FontWeight.SemiBold) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) C.TEAL.copy(alpha = 0.18f) else Color(0x0DFFFFFF))
            .border(1.dp, if (selected) C.TEAL.copy(alpha = 0.6f) else C.LINE, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { T(text, 12.sp, if (selected) C.TEAL else C.DIM, FontWeight.SemiBold) }
}

@Composable
fun Dot(color: Color, size: Dp = 9.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun KeyValue(k: String, v: String, vColor: Color = C.TEXT) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        T(k, 13.sp, C.DIM, modifier = Modifier.width(150.dp))
        T(v, 13.sp, vColor, FontWeight.Medium, Modifier.weight(1f))
    }
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        T(text, 13.sp, C.DIM, align = TextAlign.Center)
    }
}

/** Barra horizontal com valor 0..1 */
@Composable
fun Bar(frac: Float?, color: Color, modifier: Modifier = Modifier, h: Dp = 8.dp) {
    val f by animateFloatAsState((frac ?: 0f).coerceIn(0f, 1f), tween(300), label = "bar")
    Box(modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(C.TRACK)) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(h)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)))
        )
    }
}

@Composable
fun Spark(data: FloatArray, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (data.size < 2) return@Canvas
        var mn = data[0]
        var mx = data[0]
        for (x in data) {
            mn = min(mn, x)
            mx = max(mx, x)
        }
        if (mx - mn < 1e-6f) {
            mx = mn + 1f
        }
        val path = Path()
        val fill = Path()
        for (i in data.indices) {
            val x = size.width * i / (data.size - 1)
            val y = size.height - (data[i] - mn) / (mx - mn) * size.height * 0.9f - size.height * 0.05f
            if (i == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(path, color, style = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Gráfico grande com grade, para o modo Oficina. */
@Composable
fun BigChart(series: List<Pair<FloatArray, Color>>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val pad = 34f * density / 2.6f
        var mn = Float.MAX_VALUE
        var mx = -Float.MAX_VALUE
        for ((d, _) in series) for (x in d) {
            mn = min(mn, x)
            mx = max(mx, x)
        }
        if (mn == Float.MAX_VALUE) return@Canvas
        if (mx - mn < 1e-6f) mx = mn + 1f
        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(C.LINE, Offset(pad * 2, y), Offset(size.width, y), strokeWidth = 1f)
            val value = mx - (mx - mn) * i / 4f
            drawText(
                measurer, Fmt.num(value.toDouble(), if (mx - mn < 10) 1 else 0),
                topLeft = Offset(0f, y - 8f * density / 2.6f),
                style = TextStyle(color = C.DIM, fontSize = 9.sp)
            )
        }
        for ((d, col) in series) {
            if (d.size < 2) continue
            val path = Path()
            for (i in d.indices) {
                val x = pad * 2 + (size.width - pad * 2) * i / (d.size - 1)
                val y = size.height - (d[i] - mn) / (mx - mn) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, col, style = Stroke(width = 2.6f * density / 2.6f * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Mostrador principal: muda de desenho conforme o tema (arco, digital segmentado ou analógico). */
@Composable
fun RpmGauge(
    rpm: Float?,
    speed: Float?,
    modifier: Modifier = Modifier,
    maxRpm: Float = 7000f,
    sub: String = ""
) {
    val frac by animateFloatAsState(((rpm ?: 0f) / maxRpm).coerceIn(0f, 1f), tween(220), label = "rpm")
    val measurer = rememberTextMeasurer()
    val style = Themes.cur.gauge
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = size.width / 2f
            val start = 135f
            val sweep = 270f
            if (style == 1) {
                // ---- digital segmentado ----
                val stroke = size.minDimension * 0.085f
                val inset = stroke * 1.1f
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val tl = Offset(inset, inset)
                val n = 54
                val per = sweep / n
                val lit = (frac * n)
                for (i in 0 until n) {
                    val p = i / (n - 1f)
                    val col = when {
                        p < 0.55f -> lerp(C.TEAL, C.SKY, p / 0.55f)
                        p < 0.82f -> lerp(C.SKY, C.WARN, (p - 0.55f) / 0.27f)
                        else -> lerp(C.WARN, C.BAD, (p - 0.82f) / 0.18f)
                    }
                    val on = i < lit
                    val a0 = start + per * i + per * 0.12f
                    if (on) {
                        drawArc(
                            col.copy(alpha = 0.22f), a0, per * 0.76f, false, tl, arcSize,
                            style = Stroke(stroke * 1.7f)
                        )
                    }
                    drawArc(
                        if (on) col else (if (p >= 0.82f) C.BAD.copy(alpha = 0.16f) else C.TRACK),
                        a0, per * 0.76f, false, tl, arcSize, style = Stroke(stroke)
                    )
                }
                val r2 = arcSize.width / 2f - stroke * 1.4f
                drawCircle(C.LINE, radius = r2, center = Offset(c, c), style = Stroke(1.2f * density))
                drawCircle(C.LINE.copy(alpha = 0.12f), radius = r2 * 0.80f, center = Offset(c, c), style = Stroke(1f * density))
                // marcas numéricas nos mil giros
                val cnt = (maxRpm / 1000f).toInt()
                for (i in 0..cnt) {
                    val a = Math.toRadians((start + sweep * i / cnt).toDouble())
                    val rt = r2 * 0.9f
                    val lay = measurer.measure(
                        "$i", TextStyle(color = if (i >= 6) C.BAD else C.DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    )
                    drawText(lay, topLeft = Offset(c + (rt * cos(a)).toFloat() - lay.size.width / 2f, c + (rt * sin(a)).toFloat() - lay.size.height / 2f))
                }
            } else if (style == 2) {
                // ---- analógico ----
                val bez = size.minDimension * 0.035f
                val r = size.minDimension / 2f - bez
                drawCircle(Brush.radialGradient(listOf(C.CARD1, C.BG), center = Offset(c, c), radius = r), radius = r, center = Offset(c, c))
                drawCircle(C.SKY.copy(alpha = 0.55f), radius = r, center = Offset(c, c), style = Stroke(bez))
                drawCircle(C.LINE, radius = r - bez, center = Offset(c, c), style = Stroke(1.5f * density))
                // faixa vermelha
                val redStart = start + sweep * (6000f / maxRpm)
                val rr = r - bez * 2.2f
                drawArc(
                    C.BAD, redStart, start + sweep - redStart, false,
                    Offset(c - rr, c - rr), Size(rr * 2, rr * 2), style = Stroke(bez * 0.9f)
                )
                // marcas finas a cada 200 e grossas a cada 1000
                val minor = (maxRpm / 200f).toInt()
                for (i in 0..minor) {
                    val a = Math.toRadians((start + sweep * i / minor).toDouble())
                    val major = i % 5 == 0
                    val r1 = r - bez * 1.6f
                    val r2 = r1 - bez * (if (major) 2.6f else 1.2f)
                    drawLine(
                        if (major) C.TEXT else C.FAINT,
                        Offset(c + (r1 * cos(a)).toFloat(), c + (r1 * sin(a)).toFloat()),
                        Offset(c + (r2 * cos(a)).toFloat(), c + (r2 * sin(a)).toFloat()),
                        strokeWidth = (if (major) 2.6f else 1.2f) * density
                    )
                }
                val cnt = (maxRpm / 1000f).toInt()
                for (i in 0..cnt) {
                    val a = Math.toRadians((start + sweep * i / cnt).toDouble())
                    val rt = r - bez * 6.2f
                    val lay = measurer.measure(
                        "$i", TextStyle(color = if (i >= 6) C.BAD else C.TEXT, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(lay, topLeft = Offset(c + (rt * cos(a)).toFloat() - lay.size.width / 2f, c + (rt * sin(a)).toFloat() - lay.size.height / 2f))
                }
                // ponteiro
                val ang = Math.toRadians((start + sweep * frac).toDouble())
                val len = r * 0.80f
                val tail = r * 0.14f
                drawLine(
                    Color.Black.copy(alpha = 0.5f),
                    Offset(c - (tail * cos(ang)).toFloat() + 3f, c - (tail * sin(ang)).toFloat() + 3f),
                    Offset(c + (len * cos(ang)).toFloat() + 3f, c + (len * sin(ang)).toFloat() + 3f),
                    strokeWidth = 5f * density, cap = StrokeCap.Round
                )
                drawLine(
                    C.TEAL,
                    Offset(c - (tail * cos(ang)).toFloat(), c - (tail * sin(ang)).toFloat()),
                    Offset(c + (len * cos(ang)).toFloat(), c + (len * sin(ang)).toFloat()),
                    strokeWidth = 4.2f * density, cap = StrokeCap.Round
                )
                drawCircle(C.CARD1, radius = bez * 2.8f, center = Offset(c, c))
                drawCircle(C.TEAL, radius = bez * 2.8f, center = Offset(c, c), style = Stroke(2f * density))
            } else {
                // ---- arco (Carbono) ----
                val stroke = size.minDimension * 0.075f
                val inset = stroke * 1.2f
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val tl = Offset(inset, inset)
                drawArc(
                    color = C.TRACK, startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = tl, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                )
                val grad = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0f to C.BAD, 0.125f to C.BAD, 0.375f to C.TEAL,
                        0.6f to C.SKY, 0.92f to C.WARN, 1f to C.BAD
                    ),
                    center = Offset(size.width / 2, size.height / 2)
                )
                if (frac > 0.002f) {
                    drawArc(
                        brush = grad, startAngle = start, sweepAngle = sweep * frac, useCenter = false,
                        topLeft = tl, size = arcSize, alpha = 0.22f,
                        style = Stroke(stroke * 1.9f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = grad, startAngle = start, sweepAngle = sweep * frac, useCenter = false,
                        topLeft = tl, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    val ang = Math.toRadians((start + sweep * frac).toDouble())
                    val rr = arcSize.width / 2f
                    drawCircle(
                        Color.White, radius = stroke * 0.32f,
                        center = Offset(size.width / 2f + (rr * cos(ang)).toFloat(), size.height / 2f + (rr * sin(ang)).toFloat())
                    )
                }
                val n = (maxRpm / 1000f).toInt()
                val rOut = arcSize.width / 2f - stroke * 0.85f
                for (i in 0..n) {
                    val a = Math.toRadians((start + sweep * i / n).toDouble())
                    val hot = i >= 6
                    val r1 = rOut - stroke * 0.25f
                    val r2 = rOut - stroke * 0.7f
                    drawLine(
                        if (hot) C.BAD else C.FAINT,
                        Offset(c + (r1 * cos(a)).toFloat(), c + (r1 * sin(a)).toFloat()),
                        Offset(c + (r2 * cos(a)).toFloat(), c + (r2 * sin(a)).toFloat()),
                        strokeWidth = 2f * density
                    )
                    val rt = rOut - stroke * 1.55f
                    val lay = measurer.measure(
                        "$i", TextStyle(color = if (hot) C.BAD else C.DIM, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    )
                    drawText(lay, topLeft = Offset(c + (rt * cos(a)).toFloat() - lay.size.width / 2f, c + (rt * sin(a)).toFloat() - lay.size.height / 2f))
                }
            }
        }
        if (style == 2) {
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF050403))
                        .border(1.dp, C.LINE, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    T(if (speed == null) "—" else Fmt.num(speed.toDouble(), 0), 34.sp, C.TEAL, FontWeight.Bold)
                }
                T("km/h · x1000 rpm", 11.sp, C.DIM)
                if (sub.isNotEmpty()) T(sub, 11.sp, C.WARN)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                T(
                    if (speed == null) "—" else Fmt.num(speed.toDouble(), 0),
                    size = if (style == 1) 84.sp else 76.sp,
                    weight = if (style == 1) FontWeight.Medium else FontWeight.Light,
                    color = if (style == 1) C.TEAL else C.TEXT,
                    mono = style == 1
                )
                T(if (style == 1) "KM/H" else "km/h", 14.sp, C.DIM, FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                T(
                    if (rpm == null) "— rpm" else Fmt.num(rpm.toDouble(), 0) + " rpm",
                    15.sp, if (style == 1) C.SKY else C.TEAL, FontWeight.SemiBold, mono = style == 1
                )
                if (sub.isNotEmpty()) T(sub, 11.sp, C.DIM)
            }
        }
    }
}

/** Anel de pontuação (0..100) */
@Composable
fun ScoreRing(score: Int?, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(((score ?: 0) / 100f), tween(500), label = "score")
    val col = when {
        score == null -> C.DIM
        score >= 75 -> C.GOOD
        score >= 50 -> C.WARN
        else -> C.BAD
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * 0.11f
            val inset = sw
            val sz = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(C.TRACK, 135f, 270f, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(col, 135f, 270f * f, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(score?.toString() ?: "—", 30.sp, col, FontWeight.Bold)
            T("direção", 10.sp, C.DIM)
        }
    }
}

@Composable
fun RowScope.Metric(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    color: Color = C.TEXT,
    spark: FloatArray? = null,
    bar: Float? = null,
    barColor: Color = C.TEAL,
    onClick: (() -> Unit)? = null
) {
    Panel(modifier.weight(1f), pad = 12.dp, onClick = onClick) {
        Label(label)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            T(value, 26.sp, color, FontWeight.SemiBold, lines = 1)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                T(unit, 12.sp, C.DIM, modifier = Modifier.padding(bottom = 4.dp), lines = 1)
            }
        }
        if (bar != null) {
            Spacer(Modifier.height(6.dp))
            Bar(bar, barColor)
        } else if (spark != null && spark.size > 3) {
            Spacer(Modifier.height(6.dp))
            Spark(spark, barColor, Modifier.fillMaxWidth().height(26.dp))
        } else {
            Spacer(Modifier.height(32.dp))
        }
    }
}
