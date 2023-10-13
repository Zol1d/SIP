package cool.zolid.sip.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import cool.zolid.sip.R
import kotlin.math.roundToInt

val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).roundToInt()

@SuppressLint("UseCompatLoadingForDrawables") // minSdk is 24 and it is only good if minSdk is 23 or below
fun Resources.getRDrawable(@DrawableRes id: Int): Drawable = getDrawable(id, newTheme().apply {
    applyStyle(
        R.style.Theme_SIP, false
    )
})

@ColorInt
fun Context.getRColor(@ColorRes id: Int) = resources.getRColor(id)

@ColorInt
fun Resources.getRColor(@ColorRes id: Int): Int = getColor(id, newTheme().apply {
    applyStyle(
        R.style.Theme_SIP, false
    )
})

fun List<SpannableString>.join(seperator: String = "\n"): SpannableStringBuilder {
    val buffer = SpannableStringBuilder()
    forEachIndexed { index, spannableString ->
        buffer.append(spannableString)
        if (index != size - 1) buffer.append(seperator)
    }
    return buffer
}

fun Spannable.colorizeAll(@ColorRes color: Int, resources: Resources) = setSpan(
    ForegroundColorSpan(resources.getRColor(color)),
    0,
    length,
    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
)