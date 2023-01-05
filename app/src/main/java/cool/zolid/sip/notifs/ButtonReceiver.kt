package cool.zolid.sip.notifs

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cool.zolid.sip.data.ZData

class ButtonReceiver : BroadcastReceiver() {
    companion object {
        fun removeNotification(ctx: Context, id: Int) {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                id
            )
            ZData(ctx).savedNotifs = ZData(ctx).savedNotifs.apply {
                remove(find { it.date.split('.')[0].toIntOrNull() == id })
            }
        }

        fun removeNotification(ctx: Context, date: String) =
            removeNotification(ctx, date.split('.')[0].toIntOrNull() ?: 0)
    }

    override fun onReceive(context: Context, intent: Intent) =
        removeNotification(context, intent.getIntExtra("id", 0))
}