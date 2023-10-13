package cool.zolid.sip.ui.history

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cool.zolid.sip.R
import cool.zolid.sip.data.HistoryChangeEvent
import cool.zolid.sip.data.Data
import cool.zolid.sip.ui.colorizeAll
import cool.zolid.sip.ui.join
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

data class HistoryDetailsItem(
    val date: String,
    val generalChanges: String?,
    val text: SpannableStringBuilder
)

class HistoryPagerAdapter(
    private val fa: FragmentActivity
) : FragmentStateAdapter(fa) {
    private var items = mutableListOf<HistoryDetailsItem>()

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        return HistorySliderFragment(items[position])
    }

    override fun containsItem(itemId: Long): Boolean = itemId < items.size

    fun getItemPositionFromDate(date: String): Int {
        val historyDateToSet = items.find { it.date == date }
        return if (historyDateToSet != null) items.indexOf(historyDateToSet) else -1
    }

    fun setItemByDate(date: String) {
        val historyPosToSet = getItemPositionFromDate(date)
        fa.findViewById<ViewPager2>(R.id.history_details_fragment).currentItem = historyPosToSet
        (fa.findViewById<RecyclerView>(R.id.history_pane).adapter as HistoryPaneAdapter).setActivePosition(
            historyPosToSet
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(baseMap: Map<String, Map<String, String>>? = null) {
        val ccs = Data(fa).currentClassSet
        items = (baseMap ?: Data(fa).History().getAll())
            .mapValues { it.value.toMutableMap() }.toList()
            .sortedWith(
                compareBy { it.first.split('.')[0].toIntOrNull() ?: 0 }).asReversed()
            .map { change ->
                HistoryDetailsItem(
                    change.first,
                    change.second.remove("Vispārīgi"),
                    change.second.map {
                        val line = SpannableString(it.key.uppercase() + ": " + it.value.trim()
                            .replaceFirstChar { it.uppercaseChar() })
                        if (it.key in ccs) line.colorizeAll(R.color.yellow, fa.resources)
                        line
                    }.join()
                )
            }.toMutableList()
        notifyDataSetChanged()
    }

    protected fun finalize() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoyChangeEvent(event: HistoryChangeEvent) {
        submitList(event.data)
    }

    init {
        submitList()
        EventBus.getDefault().register(this)
    }
}