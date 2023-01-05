package cool.zolid.sip.data

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KProperty


class ZPrefVars(ctx: Context) {
    companion object {
        val JSONObject.toMap
            get(): Map<String, *> = keys().asSequence().associateWith {
                when (val value = this[it]) {
                    is JSONArray -> {
                        val map =
                            (0 until value.length()).associate { Pair(it.toString(), value[it]) }
                        JSONObject(map).toMap.values.toList()
                    }
                    is JSONObject -> value.toMap
                    JSONObject.NULL -> null
                    else -> value
                }
            }
    }

    private val prefs = ctx.getSharedPreferences("ZPrefs", Context.MODE_PRIVATE)
    private val remoteConfig = Firebase.remoteConfig

    fun clearAll() = prefs.edit().clear().commit()
    fun getAll(): MutableMap<String, *> = prefs.all

    inner class Num(private val default: Int) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            prefs.getInt(property.name, default)

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
            prefs.edit().putInt(property.name, value).apply()
    }

    inner class FirebaseNum {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            prefs.getInt(property.name, remoteConfig.getLong(property.name).toInt())

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
            prefs.edit { putInt(property.name, value) }
    }

    inner class Bool(private val default: Boolean) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            prefs.getBoolean(property.name, default)

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
            prefs.edit { putBoolean(property.name, value) }
    }

    inner class FirebaseBool {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            prefs.getBoolean(property.name, remoteConfig.getBoolean(property.name))

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
            prefs.edit { putBoolean(property.name, value).apply() }
    }

    inner class Set(
        private val default: kotlin.collections.Set<String> = setOf()
    ) {
        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>
        ): MutableSet<String> =
            prefs.getStringSet(property.name, default)!!.toMutableSet()

        operator fun setValue(
            thisRef: Any?,
            property: KProperty<*>,
            value: kotlin.collections.Set<String>
        ) =
            prefs.edit { putStringSet(property.name, value) }
    }

    inner class GsonObject<T>(
        private val typeToken: TypeToken<T>,
        private val default: T
    ) {
        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>
        ): T {
            val v = Gson().fromJson<T>(
                prefs.getString(
                    property.name,
                    Gson().toJson(default)
                )!!, typeToken.type
            )
            return v
        }

        operator fun setValue(
            thisRef: Any?,
            property: KProperty<*>,
            value: T
        ) =
            prefs.edit { putString(property.name, Gson().toJson(value)) }
    }
}
