package cool.zolid.sip.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import cool.zolid.sip.R
import cool.zolid.sip.data.AllClassesChangeEvent
import cool.zolid.sip.data.Data
import cool.zolid.sip.worker.WorkerUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ClazzButtonTable(private val viewgroup: ViewGroup) {
    protected fun finalize() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewClasses(@Suppress("UNUSED_PARAMETER") event: AllClassesChangeEvent) {
        viewgroup.removeView(viewgroup.findViewById(R.id.clazz_btn_table))
        ClazzButtonTable(viewgroup)
    }

    init {
        EventBus.getDefault().register(this)
        val isWorkScheduled = WorkerUtils(viewgroup.context).isWorkScheduled()
        val clazzBtnTable = LinearLayout(viewgroup.context).apply {
            id = R.id.clazz_btn_table
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            }
            orientation = LinearLayout.VERTICAL
        }
        val clazzmap = mutableMapOf<String, MutableList<String>>()
        Data(viewgroup.context).allClassList.forEach {
            if (clazzmap[it.split('.')[0]] == null) {
                clazzmap[it.split('.')[0]] = mutableListOf(it.split('.')[1])
            } else {
                clazzmap[it.split('.')[0]]?.add(it.split('.')[1])
            }
        }

        val onColor = viewgroup.resources.getRColor(
            R.color.dark_green
        )
        val offColor = viewgroup.resources.getRColor(
            R.color.yellow
        )
        for ((clazzesNum, clazzes) in clazzmap) {
            val tableRow = LinearLayout(viewgroup.context)
            tableRow.layoutParams =
                ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            tableRow.weightSum = clazzes.size.toFloat()
            tableRow.orientation = LinearLayout.HORIZONTAL
            clazzes.forEach {
                tableRow.addView(
                    ClazzButton(
                        viewgroup.context,
                        clazzesNum,
                        it,
                        onColor,
                        offColor,
                        isWorkScheduled
                    )
                )
            }
            clazzBtnTable.addView(tableRow)
        }
        viewgroup.addView(clazzBtnTable)
    }
}