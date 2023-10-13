package cool.zolid.sip.ui.slider

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import cool.zolid.sip.data.Data
import cool.zolid.sip.ui.slider.SliderButton.Companion.canSliderButtonBeShown

class SliderPagerAdapter(
    private val fa: FragmentActivity,
    private val openHistoryDate: String?
) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = if (canSliderButtonBeShown(Data(fa.baseContext))) 2 else 1
    override fun createFragment(position: Int): Fragment = SliderFragment(position, openHistoryDate)
}