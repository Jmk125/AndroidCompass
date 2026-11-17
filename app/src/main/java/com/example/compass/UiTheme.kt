package com.example.compass

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class RadarColors(
    val disc: Int, val ring: Int, val grid: Int, val muted: Int, val text: Int, val north: Int, val you: Int,
)

/** Colours, fonts and shapes for the whole app UI, one set per compass style. */
class UiTheme(
    val bg: Int, val surface: Int, val border: Int, val text: Int, val muted: Int, val accent: Int,
    val buttonBg: Int, val buttonText: Int, val buttonBorder: Int, val cornerDp: Int,
    val heading: Typeface, val body: Typeface, val isLight: Boolean,
    val radar: RadarColors, val radarFont: Typeface,
) {
    private fun px(context: Context, n: Int) = n * context.resources.displayMetrics.density

    fun box(context: Context, fill: Int, stroke: Int, strokeDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = px(context, cornerDp)
        setStroke(px(context, strokeDp).toInt(), stroke)
    }

    fun styleButton(context: Context, b: Button, selected: Boolean = false) {
        b.isAllCaps = false
        b.typeface = heading
        b.textSize = 16f
        b.setTextColor(buttonText)
        b.stateListAnimator = null
        b.minHeight = 0
        b.minimumHeight = 0
        val base = box(context, if (selected) surface else buttonBg, if (selected) accent else buttonBorder, if (selected) 3 else 1)
        b.background = RippleDrawable(ColorStateList.valueOf(Color.argb(60, 255, 255, 255)), base, null)
        val h = px(context, 14).toInt()
        val v = px(context, 10).toInt()
        b.setPadding(h, v, h, v)
    }

    fun newButton(context: Context, label: CharSequence, selected: Boolean = false, onClick: () -> Unit): Button =
        Button(context).also {
            it.text = label
            styleButton(context, it, selected)
            it.setOnClickListener { onClick() }
        }

    fun styleTitle(tv: TextView, sizeSp: Float) {
        tv.typeface = heading; tv.setTextColor(text); tv.textSize = sizeSp
    }

    fun styleBody(tv: TextView, sizeSp: Float, muted: Boolean = false) {
        tv.typeface = body; tv.setTextColor(if (muted) this.muted else text); tv.textSize = sizeSp
    }

    /** Window background behind the system bars, and light/dark bar icons. */
    fun applyWindow(activity: Activity) {
        val w = activity.window
        w.setBackgroundDrawable(ColorDrawable(bg))
        @Suppress("DEPRECATION")
        run { w.statusBarColor = bg; w.navigationBarColor = bg }
        WindowCompat.getInsetsController(w, w.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }

    companion object {
        private fun font(context: Context, id: Int, fallback: Typeface): Typeface =
            runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback

        fun of(context: Context, style: CompassStyle = StylePrefs.get(context)): UiTheme = when (style) {
            CompassStyle.MODERN -> UiTheme(
                bg = hexc("#0B0D10"), surface = hexc("#14171C"), border = hexc("#2A303A"),
                text = Color.WHITE, muted = hexc("#8A93A0"), accent = hexc("#FF5A4D"),
                buttonBg = hexc("#1E232A"), buttonText = Color.WHITE, buttonBorder = hexc("#3A414C"), cornerDp = 12,
                heading = Typeface.DEFAULT_BOLD, body = Typeface.DEFAULT, isLight = false,
                radar = RadarColors(hexc("#14171C"), hexc("#3A414C"), hexc("#2A303A"), hexc("#8A93A0"), Color.WHITE, hexc("#FF5A4D"), Color.WHITE),
                radarFont = Typeface.DEFAULT,
            )
            CompassStyle.OPERATION -> {
                val mono = font(context, R.font.share_tech_mono, Typeface.MONOSPACE)
                UiTheme(
                    bg = hexc("#070D0A"), surface = hexc("#0F1B15"), border = hexc("#2E4A38"),
                    text = hexc("#A8E6B0"), muted = hexc("#5E8F69"), accent = hexc("#F2B84B"),
                    buttonBg = hexc("#10201A"), buttonText = hexc("#A8E6B0"), buttonBorder = hexc("#5E8F69"), cornerDp = 2,
                    heading = mono, body = mono, isLight = false,
                    radar = RadarColors(hexc("#0B1510"), hexc("#3C5446"), hexc("#2E4A38"), hexc("#5E8F69"), hexc("#A8E6B0"), hexc("#F2B84B"), hexc("#A8E6B0")),
                    radarFont = mono,
                )
            }
            CompassStyle.GOTHIC -> UiTheme(
                bg = hexc("#0A080D"), surface = hexc("#17111C"), border = hexc("#5D574D"),
                text = hexc("#D8D0BF"), muted = hexc("#8F887A"), accent = hexc("#B3222D"),
                buttonBg = hexc("#1F1824"), buttonText = hexc("#D8D0BF"), buttonBorder = hexc("#8F887A"), cornerDp = 4,
                heading = font(context, R.font.unifraktur_cook, Typeface.SERIF), body = Typeface.SERIF, isLight = false,
                radar = RadarColors(hexc("#120D16"), hexc("#6D6978"), hexc("#2A2430"), hexc("#8F887A"), hexc("#D8D0BF"), hexc("#B3222D"), hexc("#D8D0BF")),
                radarFont = Typeface.SERIF,
            )
            CompassStyle.PIRATE -> {
                val pirata = font(context, R.font.pirata_one, Typeface.SERIF)
                UiTheme(
                    bg = hexc("#E3CF9C"), surface = hexc("#F0E2B8"), border = hexc("#8A6A38"),
                    text = hexc("#3B2412"), muted = hexc("#6B5230"), accent = hexc("#9B1C1C"),
                    buttonBg = hexc("#B98A2F"), buttonText = hexc("#2A1A08"), buttonBorder = hexc("#4A3210"), cornerDp = 8,
                    heading = pirata, body = Typeface.SERIF, isLight = true,
                    radar = RadarColors(hexc("#EAD6A2"), hexc("#8A6A38"), hexc("#C6A266"), hexc("#6B5230"), hexc("#3B2412"), hexc("#9B1C1C"), hexc("#3B2412")),
                    radarFont = pirata,
                )
            }
            CompassStyle.WESTERN -> UiTheme(
                bg = hexc("#2A1A0E"), surface = hexc("#3B2412"), border = hexc("#C79A3B"),
                text = hexc("#F2E4C2"), muted = hexc("#C9B58A"), accent = hexc("#C79A3B"),
                buttonBg = hexc("#C79A3B"), buttonText = hexc("#2A170A"), buttonBorder = hexc("#E9D6A6"), cornerDp = 6,
                heading = font(context, R.font.rye, Typeface.SERIF), body = Typeface.SERIF, isLight = false,
                radar = RadarColors(hexc("#EFE0BD"), hexc("#C79A3B"), hexc("#D6BD8A"), hexc("#7A5C34"), hexc("#3A2414"), hexc("#A3241B"), hexc("#3A2414")),
                radarFont = Typeface.create(Typeface.SERIF, Typeface.BOLD),
            )
            CompassStyle.PIXEL -> {
                val silk = font(context, R.font.silkscreen, Typeface.MONOSPACE)
                UiTheme(
                    bg = hexc("#1A1C2C"), surface = hexc("#333C57"), border = hexc("#566C86"),
                    text = hexc("#F4F4F4"), muted = hexc("#94B0C2"), accent = hexc("#EF7D57"),
                    buttonBg = hexc("#3B5DC9"), buttonText = Color.WHITE, buttonBorder = hexc("#F4F4F4"), cornerDp = 0,
                    heading = silk, body = silk, isLight = false,
                    radar = RadarColors(hexc("#1A1C2C"), hexc("#566C86"), hexc("#333C57"), hexc("#94B0C2"), hexc("#F4F4F4"), hexc("#EF7D57"), hexc("#FFCD75")),
                    radarFont = silk,
                )
            }
        }
    }
}

/**
 * Keeps [view]'s content clear of the status bar, navigation bar and cutouts. Newer Android draws
 * apps edge to edge, so without this the bottom buttons end up under the navigation buttons.
 */
fun applySystemInsets(view: View, extraTop: Int, extraBottom: Int, extraSide: Int) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        v.setPadding(b.left + extraSide, b.top + extraTop, b.right + extraSide, b.bottom + extraBottom)
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(view)
}

/** A dialog drawn in the current style: title, message, a custom view, a list of choices, and buttons. */
class ThemedDialog(private val activity: Activity, private val t: UiTheme) {

    private fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()

    fun show(
        title: String? = null,
        message: String? = null,
        body: View? = null,
        items: List<CharSequence>? = null,
        onItem: ((Int) -> Unit)? = null,
        positive: String? = null,
        onPositive: (() -> Unit)? = null,
        negative: String? = null,
    ): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
            background = t.box(activity, t.surface, t.border, 2)
        }
        if (title != null) column.addView(TextView(activity).apply {
            text = title; t.styleTitle(this, 22f); setPadding(0, 0, 0, dp(10))
        })
        if (message != null) column.addView(TextView(activity).apply {
            text = message; t.styleBody(this, 16f); setPadding(0, 0, 0, dp(8))
        })
        if (body != null) column.addView(body, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        if (items != null) {
            val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            items.forEachIndexed { i, label ->
                list.addView(TextView(activity).apply {
                    text = label; t.styleBody(this, 18f)
                    setPadding(dp(4), dp(12), dp(4), dp(12))
                    background = RippleDrawable(ColorStateList.valueOf(Color.argb(50, 128, 128, 128)), null, ColorDrawable(Color.WHITE))
                    setOnClickListener { dialog.dismiss(); onItem?.invoke(i) }
                }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
            column.addView(ScrollView(activity).apply { addView(list) }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        if (positive != null || negative != null) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(12), 0, 0)
            }
            if (negative != null) row.addView(Button(activity).apply {
                text = negative; isAllCaps = false; typeface = t.heading; textSize = 16f
                setTextColor(t.muted); background = null; stateListAnimator = null
                setOnClickListener { dialog.dismiss() }
            })
            if (positive != null) row.addView(t.newButton(activity, positive) {
                dialog.dismiss(); onPositive?.invoke()
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(8) })
            column.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        dialog.setContentView(column)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((activity.resources.displayMetrics.widthPixels * 0.9f).toInt(), WRAP_CONTENT)
            if (body != null) setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
        }
        dialog.show()
        return dialog
    }
}
