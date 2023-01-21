package cool.zolid.sip.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import cool.zolid.sip.R
import cool.zolid.sip.data.ZData
import cool.zolid.sip.notifs.ButtonReceiver
import cool.zolid.sip.ui.MainActivity
import cool.zolid.sip.ui.getZColor
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random.Default.nextInt


@SuppressLint("SimpleDateFormat")
val lvDateFmt = SimpleDateFormat("dd.MM.yyyy")

fun ContainsDateResult.getFormattedDate(): String {
    return lvDateFmt.format(
        when {
            today -> {
                Calendar.getInstance().time
            }
            tmrw -> {
                Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }.time
            }
            else -> calculateUntilMonday().time
        }
    )
}


data class ContainsDateResult(
    val any: Boolean, val today: Boolean, val tmrw: Boolean, val monday: Boolean
)

private fun calculateUntilMonday(): Calendar {
    val date = Calendar.getInstance()
    while (date[Calendar.DAY_OF_WEEK] != Calendar.MONDAY) date.add(Calendar.DATE, 1)
    return date
}

fun containsDate(html: String): ContainsDateResult {
    val todayresult = html.contains(lvDateFmt.format(Calendar.getInstance().time))
    val tmrwresult = html.contains(
        lvDateFmt.format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }.time
        )
    )
    val monresult = html.contains(lvDateFmt.format(calculateUntilMonday().time))
    return ContainsDateResult(
        todayresult || tmrwresult || monresult, todayresult, tmrwresult, monresult
    )
}

data class ZNotification(
    val date: String, val main: String, val containsDate: ContainsDateResult
)

fun notify(ctx: Context, date: String, main: String, containsDate: ContainsDateResult) =
    notify(ctx, ZNotification(date, main, containsDate))

fun notify(ctx: Context, data: ZNotification, sticky: Boolean = false) {
    if (ActivityCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val (date, main, containsDate) = data
    val zdata = ZData(ctx)
    val notifId = date.split('.')[0].toIntOrNull() ?: nextInt()
    val notif = NotificationCompat.Builder(ctx, "changes").apply {
        setSmallIcon(R.drawable.ic_notif)
        color = ctx.getZColor(R.color.dark_green)
        priority = NotificationCompat.PRIORITY_MAX
        setColorized(true)
        setLights(
            ctx.getZColor(R.color.dark_green), 1000, 500
        )
        setContentTitle("${if (containsDate.today) "Šodienas" else if (containsDate.tmrw) "Rītdienas" else if (containsDate.monday) "Pirmdienas" else return} izmaiņas ($date)")
        setContentText(main)
        setStyle(NotificationCompat.BigTextStyle()
            .bigText(if (main.length >= 300) main.take(main.take(300)
                .indexOfLast { it == '\n' } - 3)
                .plus("...\n(Nospiediet lai atvērtu visas izmaiņas)") else main))
        setContentIntent(
            PendingIntent.getActivity(
                ctx, notifId, Intent(ctx, MainActivity::class.java).apply {
                    putExtra(
                        "openHistoryDate", date
                    )
                }, PendingIntent.FLAG_IMMUTABLE
            )
        )
        if (sticky) {
            setOngoing(true)
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Sapratu",
                PendingIntent.getBroadcast(
                    ctx, notifId, Intent(ctx, ButtonReceiver::class.java).apply {
                        putExtra(
                            "id", notifId
                        )
                    }, PendingIntent.FLAG_IMMUTABLE
                )
            )
            zdata.savedNotifs = zdata.savedNotifs.apply { add(data) }
        }
    }
    //Notification id as date implementation to not send changes for the same date in different notifs
    NotificationManagerCompat.from(ctx).notify(notifId, notif.build())
    if (!sticky) {
        Thread.sleep(2000L)
        notify(ctx, data, true)
    }
}

fun errorNotify(
    ctx: Context, title: String? = null, text: String? = null, bigText: String? = null
) {
    if (ActivityCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val notif = NotificationCompat.Builder(ctx, "changes").apply {
        setSmallIcon(R.drawable.ic_notif)
        color = ctx.getZColor(android.R.color.holo_red_dark)
        setColorized(true)
        setLights(
            ctx.getZColor(android.R.color.holo_red_dark), 1000, 500
        )
        priority = NotificationCompat.PRIORITY_MAX
        setContentIntent(
            PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
        )
        setContentTitle(title ?: "Kļūda")
        setContentText(text ?: "Ir notikusi kļūda pārbaudot stundu izmaiņas")
        setStyle(
            NotificationCompat.BigTextStyle().bigText(
                bigText ?: text
                ?: "Ir notikusi kļūda pārbaudot stundu izmaiņas,\npar šo notikumu tika ziņots"
            )
        )
    }
    NotificationManagerCompat.from(ctx).notify(500, notif.build())
}

private val String.trim
    get() = removePrefix(" ").replaceFirstChar { char -> char.uppercaseChar() }.removeSuffix(" ")
private val dashRegex = Regex("\\s?-\\s?")

fun scrapeChanges(clazzArray: List<String>): MutableMap<String, MutableMap<String, String>> {
    val hourlyChanges = mutableMapOf<String, MutableMap<String, String>>()
    for (pElement in Jsoup.connect("http://r64vsk.lv").get().select(".r64-events > p")) {
        pElement.html(pElement.html().replace("&nbsp;", " "))
        if (pElement.html().isEmpty() || !containsDate(
                pElement.html()
            ).any
        ) continue
        val dateChanges = mutableMapOf<String, String>()
        var date: String? = null
        for (hc in pElement.html().replace('–', '-').split("<br>")) {
            if (hc.replace(Regex("<em>.*</em>"), "").isEmpty()) continue
            val containsResult = containsDate(hc)
            if (containsResult.any && Regex("<strong>.*</strong>").find(hc) != null) {
                date = containsResult.getFormattedDate()
                dateChanges["Vispārīgi"] =
                    hc.replace(Regex("<strong>.*</strong>").find(hc)!!.value, "")
                        .replace(dashRegex, "").removePrefix(".")
                        .replaceFirstChar { it.uppercaseChar() }
                continue
            }
            try {
                hc.split(dashRegex)[0].replace(" ", "").lowercase().split(",")
                    .filter { it.uppercase() in clazzArray }.forEach {
                        // Append if already in there
                        if (it in dateChanges) {
                            dateChanges[it] += (", " + hc.split(dashRegex).drop(1)
                                .joinToString(", ").trim)
                        } else {
                            dateChanges[it] = hc.split(dashRegex).drop(1).joinToString(", ").trim
                        }
                    }
            } catch (e: IndexOutOfBoundsException) {
                // Prolly a new line but meant for the last lines class
                try {
                    dateChanges[dateChanges.keys.last()] += (", " + hc.trim)
                } catch (e: NoSuchElementException) {
                    if ("Vispārīgi" in dateChanges) {
                        dateChanges["Vispārīgi"] += (" " + hc.trim)
                    } else {
                        // No clue what happened to the format if this triggers
                        Firebase.crashlytics.recordException(Throwable("FormatError"))
                    }
                }
            }
        }
        if (!date.isNullOrBlank()) hourlyChanges[date] = dateChanges
    }
    return hourlyChanges
}