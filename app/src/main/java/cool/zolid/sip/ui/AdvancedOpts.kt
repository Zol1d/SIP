package cool.zolid.sip.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import cool.zolid.sip.R
import cool.zolid.sip.data.ZData
import cool.zolid.sip.ui.slider.SliderButton
import cool.zolid.sip.worker.ZWorkerUtils

@SuppressLint("SetTextI18n")
fun MainActivity.renderAdvancedOpts(zdata: ZData, workMgr: ZWorkerUtils) {
    class AdvancedOptsDialog : DialogFragment() {
        private var lastToast: Toast? = null
        private fun SwitchMaterial.setExplanation(msg: String) = setOnLongClickListener {
            lastToast?.cancel()
            lastToast = Toast.makeText(
                context,
                msg,
                Toast.LENGTH_LONG
            ).apply { show() }
            true
        }

        @SuppressLint("InflateParams")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = layoutInflater.inflate(R.layout.advanced_opts_dialog, null)

            view.findViewById<SwitchMaterial>(R.id.compareToggle).apply {
                isChecked = zdata.compareChanges
                setOnCheckedChangeListener { _, isChecked ->
                    zdata.compareChanges = isChecked
                }
                setExplanation("Nesūtīs paziņojumu ja tas jau bijis kādreiz sūtīts")
            }
            view.findViewById<SwitchMaterial>(R.id.generalToggle).apply {
                isChecked = zdata.generalChanges
                setOnCheckedChangeListener { _, isChecked ->
                    zdata.generalChanges = isChecked
                }
                setExplanation("Pievienot sadaļu \"Vispārīgi\" paziņojuma beigās (kuri skolotāji nav skolā, u.c.)")
            }
            view.findViewById<SwitchMaterial>(R.id.clearNotifsToggle).apply {
                isChecked = zdata.clearTodaysNotifs
                setOnCheckedChangeListener { _, isChecked ->
                    zdata.clearTodaysNotifs = isChecked
                }
                setExplanation("Tīrīt šodienas paziņojumus, ja ir vēlāks par 16:00")
            }
            view.findViewById<SwitchMaterial>(R.id.historyPaneToggle).apply {
                isChecked = zdata.historyPane
                setOnCheckedChangeListener { _, isChecked ->
                    zdata.historyPane = isChecked
                    requireActivity().findViewById<SliderButton>(R.id.slider_button)
                        .refreshVisibility()
                }
                setExplanation("Pievieno paziņojumu vēstures logu")
            }
            view.findViewById<Button>(R.id.clear_all).apply {
                setOnClickListener {
                    workMgr.cancelWork()
                    zdata.clearAll()
                    finish()
                    startActivity(intent)
                }
            }
            view.findViewById<NumberPicker>(R.id.intPicker).apply {
                minValue = 1
                maxValue = 6
                value = zdata.checkInterval
                setOnValueChangedListener { _, _, newVal ->
                    zdata.checkInterval = newVal
                    ZWorkerUtils(applicationContext).enqueueWork()
                }
            }

            val builder = AlertDialog.Builder(requireActivity()).apply {
                setTitle("Advancētie iestatījumi")
                setView(view)
                setPositiveButton(
                    "Labi"
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            }
            return builder.create()
        }
    }

    val advOpts = TextView(applicationContext).apply {
        setOnClickListener {
            AdvancedOptsDialog().show(supportFragmentManager, "advanced_opts")
        }
        layoutParams =
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 50.toPx
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            }
        text = "Advancētie iestatījumi"
        setTypeface(typeface, Typeface.BOLD)
    }
    findViewById<ConstraintLayout>(R.id.main).addView(advOpts)
}