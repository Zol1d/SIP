package cool.zolid.sip.notifs

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cool.zolid.sip.data.Data
import cool.zolid.sip.worker.ZNotification
import cool.zolid.sip.worker.containsDate
import cool.zolid.sip.worker.notify

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val toRemove = mutableSetOf<ZNotification>()
        for (notif in Data(context).savedNotifs) {
            val cdr = containsDate(notif.date)
            if (cdr.any) notify(context, notif.date, notif.main, cdr)
            else {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                    notif.date.split('.')[0].toIntOrNull() ?: 0
                )
                toRemove.add(notif)
            }
        }
        if (toRemove.isNotEmpty()) Data(context).savedNotifs =
            Data(context).savedNotifs.apply { removeAll(toRemove) }
    }
}