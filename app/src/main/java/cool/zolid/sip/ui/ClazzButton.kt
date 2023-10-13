package cool.zolid.sip.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.google.android.material.button.MaterialButton
import cool.zolid.sip.R
import cool.zolid.sip.data.Data
import cool.zolid.sip.ui.slider.SliderButton
import cool.zolid.sip.worker.WorkerUtils

@SuppressLint("ViewConstructor", "SetTextI18n")
class ClazzButton(
    context: Context,
    clazzNum: String,
    clazzChar: String,
    @ColorInt private val onColor: Int,
    @ColorInt private val offColor: Int,
    isWorkScheduled: Boolean
) : MaterialButton(context),
    View.OnClickListener {
    private var toggled = false

    private fun setColor(btn: MaterialButton) {
        if (toggled) {
            btn.setBackgroundColor(onColor)
        } else {
            btn.setBackgroundColor(offColor)
        }
    }

    init {
        text = "$clazzNum.$clazzChar"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17F)
        setOnClickListener(this)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            weight = 1F
            marginEnd = 10
            marginStart = 10
        }
        setPadding(15, 10, 15, 10)
        toggled = isWorkScheduled && Data(context).currentClassSet.contains(text)
        setColor(this)
    }

    override fun onClick(v: View?) {
        toggled = !toggled
        setColor(v as MaterialButton)
        val zdata = Data(context)
        val workMgr = WorkerUtils(context)
        val chooseTxt =
            (context as Activity).findViewById<TextView>(R.id.chooseTxt)
        val oldccs = zdata.currentClassSet
        val newccs =
            oldccs.apply { if (toggled) add(v.text.toString()) else remove(v.text.toString()) }
        zdata.currentClassSet = newccs
        if (newccs.isEmpty()) {
            chooseTxt.text =
                "Izvēlieties savu klasi/-es:"
            workMgr.cancelWork()
        } else {
            chooseTxt.text =
                "Ieslēgts"
            workMgr.enqueueWork(oldccs.isEmpty())
        }
        (context as Activity).findViewById<SliderButton>(R.id.slider_button).refreshVisibility()
    }
}