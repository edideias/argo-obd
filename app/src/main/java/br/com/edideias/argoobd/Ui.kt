package br.com.edideias.argoobd

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

object C {
    val BG = Color.parseColor("#0A0E14")
    val CARD = Color.parseColor("#131A24")
    val CARD2 = Color.parseColor("#1D2733")
    val TEXT = Color.parseColor("#E8EEF5")
    val DIM = Color.parseColor("#7C8A9C")
    val ACCENT = Color.parseColor("#2DD4BF")
    val GOOD = Color.parseColor("#4ADE80")
    val WARN = Color.parseColor("#FBBF24")
    val BAD = Color.parseColor("#F87171")
    val COLD = Color.parseColor("#60A5FA")
}

interface Page {
    val view: View
    fun refresh()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun roundBg(color: Int, radius: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

fun Context.tv(s: String, size: Float = 14f, color: Int = C.TEXT, bold: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

fun lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }

const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

fun Context.vbox(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
fun Context.hbox(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

fun Context.card(pad: Int = 12): LinearLayout = vbox().apply {
    background = roundBg(C.CARD, dpf(16f))
    setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
}

fun Context.btn(label: String, primary: Boolean = true, onClick: () -> Unit): TextView =
    tv(label, 14f, if (primary) C.BG else C.TEXT, true).apply {
        gravity = Gravity.CENTER
        background = roundBg(if (primary) C.ACCENT else C.CARD2, dpf(12f))
        setPadding(dp(14), dp(11), dp(14), dp(11))
        setOnClickListener { onClick() }
    }

fun Context.sectionTitle(s: String): TextView =
    tv(s.uppercase(), 12f, C.DIM, true).apply {
        letterSpacing = 0.08f
        setPadding(dp(4), dp(14), 0, dp(6))
    }

/** Mostrador em arco: o arco mostra a rotação, o número grande mostra a velocidade. */
class ArcGauge(c: Context) : View(c) {
    var max = 6000f
    var value = 0f
    var big = "0"
    var small = "km/h"
    var sub = ""
    var redFrom = 0.85f

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = C.CARD2
    }
    private val prog = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onMeasure(w: Int, h: Int) {
        val ww = MeasureSpec.getSize(w)
        setMeasuredDimension(ww, (ww * 0.86f).toInt())
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val sw = w * 0.065f
        track.strokeWidth = sw
        prog.strokeWidth = sw
        val r = min(w, h) / 2f - sw
        val cx = w / 2f
        val cy = h / 2f
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        cv.drawArc(rect, 135f, 270f, false, track)
        val f = (value / max).coerceIn(0f, 1f)
        prog.color = when {
            f > redFrom -> C.BAD
            f > 0.7f -> C.WARN
            else -> C.ACCENT
        }
        if (f > 0.002f) cv.drawArc(rect, 135f, 270f * f, false, prog)
        tp.color = C.TEXT
        tp.textSize = r * 0.85f
        cv.drawText(big, cx, cy + r * 0.28f, tp)
        tp.color = C.DIM
        tp.textSize = r * 0.2f
        cv.drawText(small, cx, cy + r * 0.56f, tp)
        tp.color = C.ACCENT
        tp.textSize = r * 0.24f
        cv.drawText(sub, cx, cy + r * 0.98f, tp)
    }
}

/** Cartão com rótulo, valor grande e unidade. */
class Tile(c: Context, label: String, unit: String) : LinearLayout(c) {
    private val l = c.tv(label, 12f, C.DIM)
    private val v = c.tv("—", 28f, C.TEXT, true)
    private val u = c.tv(unit, 12f, C.DIM)

    init {
        orientation = VERTICAL
        background = roundBg(C.CARD, c.dpf(16f))
        setPadding(c.dp(14), c.dp(12), c.dp(14), c.dp(12))
        addView(l)
        addView(v)
        addView(u)
    }

    fun set(text: String, color: Int = C.TEXT) {
        v.text = text
        v.setTextColor(color)
    }

    fun setUnit(s: String) {
        u.text = s
    }

    fun setLabel(s: String) {
        l.text = s
    }
}

/** Gráfico de linha simples com escala automática. */
class Spark(c: Context) : View(c) {
    var data = FloatArray(0)
    var unit = ""

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = c.dpf(2f)
        color = C.ACCENT
        strokeJoin = Paint.Join.ROUND
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C.DIM
        textSize = c.dpf(11f)
    }
    private val grid = Paint().apply {
        color = C.CARD2
        strokeWidth = c.dpf(1f)
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = context.dpf(6f)
        cv.drawLine(0f, h / 2, w, h / 2, grid)
        if (data.size < 2) {
            cv.drawText("aguardando dados…", pad, h / 2, txt)
            return
        }
        var mn = data[0]
        var mx = data[0]
        for (x in data) {
            mn = min(mn, x)
            mx = max(mx, x)
        }
        if (mx - mn < 0.0001f) {
            mx += 1f
            mn -= 1f
        }
        val p = Path()
        for (i in data.indices) {
            val x = pad + (w - 2 * pad) * i / (data.size - 1)
            val y = pad + (h - 2 * pad) * (1 - (data[i] - mn) / (mx - mn))
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        cv.drawPath(p, line)
        cv.drawText(Fmt.num(mx.toDouble(), 1) + " " + unit, pad, pad + txt.textSize, txt)
        cv.drawText(Fmt.num(mn.toDouble(), 1) + " " + unit, pad, h - pad, txt)
    }
}
