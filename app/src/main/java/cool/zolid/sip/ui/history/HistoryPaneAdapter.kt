package cool.zolid.sip.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cool.zolid.sip.R
import cool.zolid.sip.data.HistoryChangeEvent
import cool.zolid.sip.data.ZData
import cool.zolid.sip.ui.getZColor
import cool.zolid.sip.worker.ContainsDateResult
import cool.zolid.sip.worker.containsDate
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


data class HistoryItem(
    val date: String,
    val cdr: ContainsDateResult
)

class HistoryPaneAdapter(
    private val fa: FragmentActivity,
    private var initDate: String?
) : RecyclerView.Adapter<HistoryPaneAdapter.ItemViewHolder>() {
    private val differ: AsyncListDiffer<HistoryItem> = AsyncListDiffer(this, DIFF_CALLBACK)
    private var currentItemElement: ItemViewHolder? = null

    protected fun finalize() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoyChangeEvent(event: HistoryChangeEvent) {
        submitList(event.data)
    }

    init {
        submitList(ZData(fa).History().getAll())
        EventBus.getDefault().register(this)
    }

    fun submitList(baseMap: Map<String, Map<String, String>>) {
        differ.submitList(baseMap.toList()
            .sortedWith(
                compareBy { it.first.split('.')[0].toIntOrNull() ?: 0 }).asReversed()
            .map { HistoryItem(it.first, containsDate(it.first)) })
    }

    override fun getItemCount(): Int = differ.currentList.size

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<HistoryItem> =
            object : DiffUtil.ItemCallback<HistoryItem>() {
                override fun areContentsTheSame(
                    oldItem: HistoryItem,
                    newItem: HistoryItem
                ): Boolean = oldItem == newItem

                override fun areItemsTheSame(
                    oldItem: HistoryItem,
                    newItem: HistoryItem
                ): Boolean = oldItem == newItem
            }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.date)
        val additionalInfo: TextView = view.findViewById(R.id.additional_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_pane_item, parent, false)

        return ItemViewHolder(adapterLayout)
    }

    fun setActivePosition(position: Int) {
        currentItemElement?.date?.setTextColor(Color.parseColor("#E6000000"))
        val currViewHolder = (fa.findViewById<RecyclerView>(R.id.history_pane)
            .findViewHolderForAdapterPosition(position) as ItemViewHolder)
        currViewHolder.date.setTextColor(
            fa.getZColor(
                R.color.yellow
            )
        )
        currentItemElement = currViewHolder
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.date.text = item.date
        if (item.cdr.any) {
            holder.additionalInfo.visibility = View.VISIBLE
            holder.additionalInfo.text =
                if (item.cdr.today) "Šodienas" else if (item.cdr.tmrw) "Rītdienas" else if (item.cdr.monday) "Nāk. pirmdienas" else ""
        }
        holder.itemView.setOnClickListener {
            (fa.findViewById<ViewPager2>(R.id.history_details_fragment)?.adapter as? HistoryPagerAdapter)?.setItemByDate(
                item.date
            )
        }
        if (item.date == initDate) {
            holder.date.setTextColor(
                fa.getZColor(
                    R.color.yellow
                )
            )
            initDate = null
        }
    }
}