package cool.zolid.sip.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import cool.zolid.sip.BuildConfig
import cool.zolid.sip.R
import cool.zolid.sip.data.AllClassesChangeEvent
import cool.zolid.sip.data.ZData
import cool.zolid.sip.ui.slider.SliderPagerAdapter
import cool.zolid.sip.worker.ZWorkerUtils
import org.greenrobot.eventbus.EventBus
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var workMgr: ZWorkerUtils
    private lateinit var zdata: ZData

    private fun displayAuthFail() {
        setContentView(R.layout.error)
        findViewById<ImageButton>(R.id.errorBtn).setOnClickListener {
            finish()
            startActivity(intent)
        }
        Firebase.analytics.logEvent("v2AuthFail", null)
    }

    private fun startAuth(): Boolean {
        var trys = 6
        while (trys > 0) {
            if (remoteConfig.getBoolean("online")) return true
            Thread.sleep(1000L)
            trys--
        }
        return false
    }

    @SuppressLint("SetTextI18n", "InflateParams", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteConfig = Firebase.remoteConfig
        workMgr = ZWorkerUtils(applicationContext)
        zdata = ZData(applicationContext)


        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.updatePriority() >= 3 && (appUpdateInfo.isUpdateTypeAllowed(
                    AppUpdateType.IMMEDIATE
                ) || appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE,
                    this,
                    100
                )
            }
        }

        remoteConfig.setDefaultsAsync(
            mapOf(
                "online" to true,
                "showAdvancedOpts" to false,
                "checkInterval" to 2,
                "historyPane" to true,
                "clearTodaysNotifs" to false,
                "stickyNotifs" to true,
                "clearTodaysNotifsHour" to 15
            )
        )
        Firebase.database("https://sip2-6559c-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("classes").get().addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val res = task.result.children.map {
                            it.getValue<String>()!!
                        }
                        zdata.allClassList = res
                        zdata.currentClassSet =
                            zdata.currentClassSet.filter { it in res }.toMutableSet()
                        EventBus.getDefault().post(AllClassesChangeEvent(res))
                    } else {
                        val res = zdata.allClassList
                        zdata.currentClassSet =
                            zdata.currentClassSet.filter { it in res }.toMutableSet()
                    }
                } catch (e: Throwable) {
                    Firebase.crashlytics.recordException(e)
                    Toast.makeText(this, "Nevarēja ielādēt klases", Toast.LENGTH_SHORT).show()
                }
            }
        remoteConfig.fetch(if (BuildConfig.DEBUG) 0 else 3600)
            .addOnSuccessListener { remoteConfig.activate() }

        NotificationManagerCompat.from(applicationContext).apply {
            cancel(500)
            cancel(100)
        }

        if (!startAuth()) {
            displayAuthFail()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "changes", "Stundu izmaiņas", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                lightColor = resources.getZColor(R.color.dark_green)
                enableLights(true)
                enableVibration(true)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
        }

        val activView = layoutInflater.inflate(R.layout.activity_main, null) as ConstraintLayout

        activView.findViewById<TextView>(R.id.buildNum).text =
            "SIP versija ${BuildConfig.VERSION_NAME}"

        val cal = Calendar.getInstance()
        activView.findViewById<TextView>(R.id.creditText).text = "Kodējis Matīss no ${
            (if (cal.get(Calendar.DAY_OF_YEAR) > 244) cal.get(Calendar.YEAR) + 1 else cal.get(
                Calendar.YEAR
            )) - 2015
        }.D"
        activView.findViewById<ViewPager2>(R.id.pager).let {
            it.adapter = SliderPagerAdapter(
                this, intent.getStringExtra(
                    "openHistoryDate"
                )
            )
            it.isUserInputEnabled = false
            it.currentItem = when {
                intent.getBooleanExtra(
                    "forceMainScreenLoad", false
                ) -> 0
                intent.getBooleanExtra(
                    "forceHistoryScreenLoad", false
                ) || intent.getStringExtra(
                    "openHistoryDate"
                ) != null -> 1
                zdata.lastSliderPageMain -> 0
                else -> 1
            }
            zdata.lastSliderPageMain = it.currentItem == 0
        }


        setTheme(R.style.Theme_SIP)
        setContentView(activView)

        if (zdata.showAdvancedOpts) renderAdvancedOpts(zdata, workMgr)
    }

    @SuppressLint("BatteryLife")
    override fun onResume() {
        super.onResume()
        val isWorkScheduled = ZWorkerUtils(applicationContext).isWorkScheduled()
        findViewById<TextView>(R.id.chooseTxt)?.text =
            if (isWorkScheduled) "Ieslēgts" else "Izvēlieties savu klasi/-es:"
        if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                packageName
            )
        ) startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
        ) else if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0
                )
            }
        }
    }
}
