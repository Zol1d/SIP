package cool.zolid.sip.worker

import android.content.Context
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import cool.zolid.sip.BuildConfig
import cool.zolid.sip.data.ZData
import cool.zolid.sip.notifs.ButtonReceiver.Companion.removeNotification
import org.jsoup.HttpStatusException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit

class ZWorkerUtils(private val ctx: Context) {
    private val mgr = WorkManager.getInstance(ctx)

    fun isWorkScheduled(): Boolean {
        return try {
            val state = mgr.getWorkInfosForUniqueWork("hourlyChanges").get()[0].state
            state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
        } catch (e: IndexOutOfBoundsException) {
            false
        } catch (e: Throwable) {
            Firebase.crashlytics.recordException(e)
            false
        }
    }

    fun cancelWork() {
        mgr.cancelUniqueWork("hourlyChanges")
    }

    fun enqueueWork(forceDelay: Boolean = false) {
        class ChangesWorker(appContext: Context, workerParams: WorkerParameters) :
            Worker(appContext, workerParams) {
            fun successWithoutNotif(): Result {
                Firebase.analytics.logEvent("success", null)
                ZData(applicationContext).errored = false
                return Result.success()
            }

            override fun doWork(): Result {
                Firebase.remoteConfig.fetch(43200)
                    .addOnSuccessListener { Firebase.remoteConfig.activate() }

                if (!Firebase.remoteConfig.getBoolean("online")) {
                    errorNotify(
                        applicationContext,
                        "SIP ir izslēgts",
                        "SIP tika izslēgts dēļ nezināma iemesla, turpmāk paziņojumus vairs nesaņemsiet"
                    )
                    WorkManager.getInstance(applicationContext)
                        .cancelUniqueWork("hourlyChanges")
                    return Result.failure()
                }

                if (!(applicationContext.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                        applicationContext.packageName
                    )
                ) {
                    errorNotify(
                        applicationContext,
                        "Kļūda, nospiediet lai salabotu",
                        "SIP nav atļaujas lai strādātu fonā, tādēļ nevar pārbaudīt izmaiņas"
                    )
                    Firebase.analytics.logEvent("batteryCheckFailedInWorker", null)
                    return Result.failure()
                }
                val zdata = ZData(applicationContext)

                if (zdata.clearTodaysNotifs && Calendar.getInstance()
                        .get(Calendar.HOUR_OF_DAY) > Firebase.remoteConfig.getLong("clearTodaysNotifsHour")
                ) {
                    zdata.savedNotifs.iterator().forEach {
                        if (it.date == lvDateFmt.format(Calendar.getInstance().time)) {
                            removeNotification(applicationContext, it.date)
                        }
                    }
                }
                try {
                    val rawchanges = scrapeChanges(
                        zdata.allClassList
                    )
                    if (rawchanges.isEmpty()) return successWithoutNotif()
                    val ccs = zdata.currentClassSet.map { it.lowercase() }
                    val history = zdata.History()
                    for ((date, rawdatechanges) in rawchanges) {
                        val general = rawdatechanges["Vispārīgi"]
                        val changes =
                            rawdatechanges.filterKeys {
                                ccs.contains(
                                    Regex("\\d{1,2}\\.?\\w{1,3}").find(
                                        it
                                    )?.value
                                )
                            }
                        if (changes.isEmpty()) continue
                        if (zdata.compareChanges) {
                            val histdata = history.getWhen(date)
                            if (!(histdata == null || changes.any { histdata[it.key] != it.value })) continue
                            history.make(date, rawdatechanges)
                        }
                        var notifTxt = ""
                        changes.forEach {
                            notifTxt += it.key + ": " + it.value.trim()
                                .replaceFirstChar { it.uppercaseChar() } + "\n"
                        }
                        if (!general.isNullOrBlank() && zdata.generalChanges) notifTxt += "Vispārīgi: $general"
                        notifTxt = notifTxt.trim().trimEnd('\n')
                        val containsDate = containsDate(date)
                        Firebase.analytics.logEvent("successWithNotif", null)
                        notify(
                            applicationContext,
                            date,
                            notifTxt,
                            containsDate
                        )
                    }
                    zdata.errored = false
                    return Result.success()
                } catch (e: HttpStatusException) {
                    Firebase.analytics.logEvent("websiteError", null)
                    return Result.retry()
                } catch (e: Throwable) {
                    if (listOf(
                            IOException::class,
                            UnknownHostException::class,
                            SocketTimeoutException::class,
                            SocketException::class,
                            ConnectException::class
                        ).contains(e::class)
                    ) {
                        // Network error
                        Firebase.analytics.logEvent(e::class.simpleName.toString(), null)
                        Firebase.analytics.logEvent("netError", null)
                        return Result.retry()
                    }
                    val crashlytics = Firebase.crashlytics
                    zdata.setFirebaseKeys()
                    crashlytics.log(zdata.History().toString())
                    Firebase.messaging.token.addOnCompleteListener {
                        if (it.isSuccessful) crashlytics.setCustomKey("FCM token", it.result)
                        crashlytics.recordException(e)
                    }
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                        errorNotify(applicationContext, e.toString())
                    } else if (!zdata.errored) errorNotify(applicationContext)
                    zdata.errored = true
                    return Result.failure()
                }
            }
        }

        val zdata = ZData(ctx)
        mgr.enqueueUniquePeriodicWork(
            "hourlyChanges",
            ExistingPeriodicWorkPolicy.REPLACE,
            PeriodicWorkRequestBuilder<ChangesWorker>(
                zdata.checkInterval.toLong(),
                TimeUnit.HOURS
            )
                .apply {
                    setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    if (zdata.compareChanges && forceDelay) setInitialDelay(3, TimeUnit.MINUTES)
                }
                .build()
        )
//        Firebase.analytics.logEvent("classesArrayUpdate", Bundle().apply {
//            putStringArrayList("Klases", ArrayList(zdata.currentClassSet))
//        })
    }
}
