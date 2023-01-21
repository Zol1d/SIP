package cool.zolid.sip

import android.app.Application
import org.greenrobot.eventbus.EventBus

class SIPApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EventBus.builder().addIndex(SIPEventBusIndex()).installDefaultEventBus()
    }
}