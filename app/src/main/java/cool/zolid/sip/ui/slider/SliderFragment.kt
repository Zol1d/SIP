package cool.zolid.sip.ui.slider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cool.zolid.sip.R
import cool.zolid.sip.ui.ClazzButtonTable
import cool.zolid.sip.ui.getRDrawable
import cool.zolid.sip.ui.history.HistoryPagerAdapter
import cool.zolid.sip.ui.history.HistoryPaneAdapter

class SliderFragment(private val pos: Int, private val openHistoryDate: String?) : Fragment() {
    private fun createHistoryPane(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.BELOW, R.id.slider_button)
                marginStart = 10
                marginEnd = 10
            }
            addView(RecyclerView(requireContext()).apply {
                layoutManager = LinearLayoutManager(requireContext())
                background = resources.getRDrawable(R.drawable.green_border_no_bottom)
                adapter = HistoryPaneAdapter(requireActivity(), openHistoryDate)
                id = R.id.history_pane
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            })
            addView(
                ViewPager2(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    adapter = HistoryPagerAdapter(requireActivity())
                    id = R.id.history_details_fragment
                    background = resources.getRDrawable(R.drawable.module_background)
                    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            (requireActivity().findViewById<RecyclerView>(R.id.history_pane)?.adapter as? HistoryPaneAdapter)?.setActivePosition(
                                position
                            )
                        }
                    })
                    if (openHistoryDate != null) currentItem =
                        (adapter as HistoryPagerAdapter).getItemPositionFromDate(openHistoryDate)
                }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainLayout =
            inflater.inflate(R.layout.fragment_base_layout, container, false) as RelativeLayout
        mainLayout.addView(SliderButton(this, pos))
        if (pos == 0) ClazzButtonTable(mainLayout)
        else mainLayout.addView(createHistoryPane())
        (mainLayout.findViewById<ViewPager2>(R.id.history_details_fragment)?.adapter as? HistoryPagerAdapter)?.notifyItemChanged(
            0
        )
        return mainLayout
    }
}