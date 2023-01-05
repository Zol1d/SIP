package cool.zolid.sip.ui.slider

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import cool.zolid.sip.R
import cool.zolid.sip.data.HistoryChangeEvent
import cool.zolid.sip.data.ZData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


@SuppressLint("ViewConstructor", "SetTextI18n")
class SliderButton(
    private val fragment: Fragment,
    pos: Int
) : MaterialButton(fragment.requireContext()),
    View.OnClickListener {
    private val zdata = ZData(context)
    private val pagerAdapter =
        fragment.requireActivity().findViewById<ViewPager2>(R.id.pager).adapter!!

    private fun updatePager(historyPane: Boolean) = Handler(Looper.getMainLooper()).post {
        if (historyPane) pagerAdapter.notifyItemInserted(1) else pagerAdapter.notifyItemRemoved(1)
    }

    companion object {
        fun canSliderButtonBeShown(
            zdata: ZData,
            newHistory: Map<String, Map<String, String>>? = null
        ): Boolean {
            val ccs = zdata.currentClassSet.map { it.lowercase() }
            return zdata.historyPane && (newHistory ?: zdata.History()
                .getAll()).values.any { it.keys.any { it in ccs } }
        }
    }

    protected fun finalize() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoyChangeEvent(event: HistoryChangeEvent) = refreshVisibility(event.data)


    private var isDisabled
        get() = alpha != 1F
        set(value) {
            alpha = if (value) .3F else 1F
        }

    fun refreshVisibility(newHistory: Map<String, Map<String, String>>? = null) {
        if (zdata.historyPane) {
            visibility = VISIBLE
        } else {
            visibility = GONE
            return
        }
        val canBeShown = canSliderButtonBeShown(zdata, newHistory)
        if (canBeShown && isDisabled) {
            isDisabled = false
            updatePager(true)
        } else if (!canBeShown && !isDisabled) {
            isDisabled = true
            updatePager(false)
        }
    }

    init {
        EventBus.getDefault().register(this)
        refreshVisibility()
        text = if (pos == 0) "Izmaiņu vēsture" else "Atpakaļ"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17F)
        setOnClickListener(this)
        id = R.id.slider_button
        layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(
                if (pos == 0) RelativeLayout.ALIGN_PARENT_RIGHT else RelativeLayout.ALIGN_PARENT_LEFT,
                RelativeLayout.TRUE
            )
            marginEnd = 10
            marginStart = 10
        }
        setPadding(15, 10, 15, 10)
    }

    override fun onClick(v: View) {
        if (isDisabled) return Toast.makeText(
            context,
            "No tavām izvēlētajām klasēm nevienai nav vēsture",
            Toast.LENGTH_LONG
        ).show()
        val newToggle = !zdata.lastSliderPageMain
        zdata.lastSliderPageMain = newToggle
        fragment.requireActivity().findViewById<ViewPager2>(R.id.pager).currentItem =
            if (newToggle) 0 else 1
    }
}