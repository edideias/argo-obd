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

object C {
    val BG = Color(0xFF05080D)
    val BG2 = Color(0xFF0A111A)
    val CARD1 = Color(0xFF141D29)
    val CARD2 = Color(0xFF0E151F)
    val LINE = Color(0x1FFFFFFF)
    val TEXT = Color(0xFFEAF1F8)
    val DIM = Color(0xFF8493A6)
    val FAINT = Color(0xFF4A5868)
    val TEAL = Color(0xFF2DD4BF)
    val SKY = Color(0xFF38BDF8)
    val GOOD = Color(0xFF4ADE80)
    val WARN = Color(0xFFFBBF24)
    val BAD = Color(0xFFF87171)
    val COLD = Color(0xFF60A5FA)
    val TRACK = Color(0xFF18222E)
}

val Brand = Brush.horizontalGradient(listOf(C.TEAL, C.SKY))

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
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif
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
    val shape = RoundedCornerShape(22.dp)
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
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (enabled) Brand else Brush.horizontalGradient(listOf(C.TRACK, C.TRACK)))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) { T(text, 14.sp, if (enabled) Color(0xFF04201C) else C.DIM, FontWeight.Bold) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = C.TEXT, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
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

/** Mostrador principal: giro no arco, velocidade no centro. */
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
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.075f
            val inset = stroke * 1.2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val tl = Offset(inset, inset)
            val start = 135f
            val sweep = 270f
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
                val cx = size.width / 2f + (rr * cos(ang)).toFloat()
                val cy = size.height / 2f + (rr * sin(ang)).toFloat()
                drawCircle(Color.White, radius = stroke * 0.32f, center = Offset(cx, cy))
            }
            // marcas de 1000 em 1000
            val n = (maxRpm / 1000f).toInt()
            val rOut = arcSize.width / 2f - stroke * 0.85f
            for (i in 0..n) {
                val a = Math.toRadians((start + sweep * i / n).toDouble())
                val hot = i >= 6
                val r1 = rOut - stroke * 0.25f
                val r2 = rOut - stroke * 0.7f
                val c = size.width / 2f
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
                drawText(
                    lay,
                    topLeft = Offset(
                        c + (rt * cos(a)).toFloat() - lay.size.width / 2f,
                        c + (rt * sin(a)).toFloat() - lay.size.height / 2f
                    )
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(
                if (speed == null) "—" else Fmt.num(speed.toDouble(), 0),
                size = 76.sp, weight = FontWeight.Light, color = C.TEXT
            )
            T("km/h", 14.sp, C.DIM, FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            T(
                if (rpm == null) "— rpm" else Fmt.num(rpm.toDouble(), 0) + " rpm",
                15.sp, C.TEAL, FontWeight.SemiBold
            )
            if (sub.isNotEmpty()) T(sub, 11.sp, C.DIM)
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
