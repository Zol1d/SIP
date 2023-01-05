package cool.zolid.sip.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import cool.zolid.sip.R

class HistorySliderFragment(private val item: HistoryDetailsItem) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.history_details, container, false).apply {
        findViewById<TextView>(R.id.titleText).text = item.date
        if (item.generalChanges != null) {
            findViewById<TextView>(R.id.generalText).apply {
                text = item.generalChanges
                visibility = View.VISIBLE
            }
        }
        findViewById<TextView>(R.id.contentText).text = item.text
        return this
    }
}