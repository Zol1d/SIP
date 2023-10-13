package cool.zolid.sip.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import cool.zolid.sip.R
import cool.zolid.sip.data.PrefVars.Companion.toMap
import cool.zolid.sip.worker.ZNotification
import cool.zolid.sip.worker.lvDateFmt
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.text.ParseException
import java.util.*


class Data(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("hourlyChanges", Context.MODE_PRIVATE)
    private val eventBus = EventBus.getDefault()

    @SuppressLint("ApplySharedPref")
    fun clearAll() {
        prefs.edit().clear().commit()
        ZVar.clearAll()
    }

    @Suppress("UNCHECKED_CAST")
    inner class History {
        fun getWhen(date: String): Map<String, String>? {
            val hist = JSONObject(
                prefs.getString(
                    "history",
                    JSONObject().toString()
                )!!
            ).toMap as MutableMap<String, Map<String, String>>
            //                                                             5 days (in miliseconds)
            val unixTime = Date(Calendar.getInstance().timeInMillis - 4 * 1000 * 60 * 60 * 24)
            hist.entries.removeIf {
                return@removeIf try {
                    lvDateFmt.parse(it.key)!!.before(unixTime)
                } catch (e: ParseException) {
                    true
                }
            }
            prefs.edit().putString("history", JSONObject(hist as Map<*, *>).toString()).apply()
            return hist[date]
        }

        fun getAll(): Map<String, Map<String, String>> {
            return JSONObject(
                prefs.getString(
                    "history",
                    JSONObject().toString()
                )!!
            ).toMap as Map<String, Map<String, String>>
        }

        fun make(date: String, changes: Map<String, String>) {
            val prev = JSONObject(prefs.getString("history", JSONObject().toString())!!)
            prev.put(date, JSONObject(changes))
            prefs.edit().putString("history", prev.toString()).apply()
            eventBus.post(HistoryChangeEvent(prev.toMap as Map<String, Map<String, String>>))
        }

        override fun toString(): String {
            return prefs.getString("history", JSONObject().toString())!!
        }
    }

    fun setFirebaseKeys() = ZVar.getAll().forEach {
        Firebase.crashlytics.setCustomKey(it.key, it.value.toString())
    }

    private val ZVar = PrefVars(ctx)
    var compareChanges by ZVar.Bool(true)
    var errored by ZVar.Bool(false)
    var generalChanges by ZVar.Bool(true)
    var clearTodaysNotifs by ZVar.FirebaseBool()
    var historyPane by ZVar.FirebaseBool()
    var checkInterval by ZVar.FirebaseNum()
    var currentClassSet by ZVar.Set()
    var lastSliderPageMain by ZVar.Bool(true)
    var showAdvancedOpts by ZVar.FirebaseBool()
    var allClassList by ZVar.GsonObject(
        object : TypeToken<List
        <String>>() {},
        ctx.resources.getStringArray(R.array.backupClasses).toList(),
    )
    var savedNotifs by ZVar.GsonObject(
        object : TypeToken<MutableSet<ZNotification>>() {},
        mutableSetOf(),
    )
}