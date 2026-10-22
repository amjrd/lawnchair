package app.lawnchair.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import app.lawnchair.font.FontCache
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherPrefs
import java.util.concurrent.CopyOnWriteArraySet
import org.json.JSONObject

sealed class BasePreferenceManager(private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    val sp: SharedPreferences = LauncherPrefs.getPrefs(context)
    val prefsMap = mutableMapOf<String, BasePref<*>>()

    private var changedPrefs: MutableSet<BasePref<*>>? = null
    private var editor: SharedPreferences.Editor? = null
    private var inBatchMode = false
        set(value) {
            if (field != value) {
                if (field) {
                    editor!!.apply()
                    editor = null
                    val tmp = changedPrefs?.let { HashSet(it) }
                    changedPrefs = null
                    if (tmp != null) {
                        tmp.forEach { it.invalidate() }
                        tmp.forEach { it.onSharedPreferenceChange() }
                    }
                }
                field = value
            }
        }

    fun batchEdit(block: () -> Unit) {
        val wasInBatchMode = inBatchMode
        try {
            inBatchMode = true
            block()
        } finally {
            inBatchMode = wasInBatchMode
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String?) {
        key?.let { prefsMap[it] }?.let {
            if (inBatchMode) {
                (changedPrefs ?: HashSet<BasePref<*>>().also { s -> changedPrefs = s }).add(it)
            } else {
                it.invalidate()
                it.onSharedPreferenceChange()
            }
        }
    }

    private fun edit(block: (SharedPreferences.Editor) -> Unit) {
        if (inBatchMode) {
            (editor ?: sp.edit().also { editor = it }).apply(block)
        } else {
            sp.edit(action = block)
        }
    }

    protected val idp get() = InvariantDeviceProfile.INSTANCE.get(context)

    abstract inner class BasePref<T : Any?>(
        val key: String,
        val defaultValue: T,
        private val onSet: () -> Unit,
    ) {
        protected var isInitialized = false
        protected var cachedValue: T? = null
        val listeners = CopyOnWriteArraySet<PreferenceChangeListener<T>>()

        init {
            @Suppress("LeakingThis")
            prefsMap[key] = this
        }

        fun invalidate() {
            isInitialized = false
            cachedValue = null
        }

        fun onSharedPreferenceChange() {
            listeners.forEach { it.onPreferenceChange(get()) }
        }

        fun get(): T {
            if (!isInitialized) {
                cachedValue = read()
                isInitialized = true
            }
            @Suppress("UNCHECKED_CAST")
            return cachedValue as T
        }

        operator fun invoke() = get()

        fun set(value: T) {
            if (value == get()) return
            edit { write(it, value) }
            onSet()
        }

        fun addListener(listener: PreferenceChangeListener<T>) {
            listeners.add(listener)
        }

        fun removeListener(listener: PreferenceChangeListener<T>) {
            listeners.remove(listener)
        }

        fun addListener(listener: (T) -> Unit): PreferenceChangeListener<T> {
            val preferenceChangeListener = PreferenceChangeListener(listener)
            addListener(preferenceChangeListener)
            return preferenceChangeListener
        }

        abstract fun read(): T
        abstract fun write(editor: SharedPreferences.Editor, value: T)
    }

    inner class StringPref(
        key: String,
        defaultValue: String = "",
        onSet: () -> Unit = {},
    ) : BasePref<String>(key, defaultValue, onSet) {
        override fun read(): String = sp.getString(key, defaultValue) ?: defaultValue
        override fun write(editor: SharedPreferences.Editor, value: String) {
            editor.putString(key, value)
        }
    }

    inner class BoolPref(
        key: String,
        defaultValue: Boolean = false,
        onSet: () -> Unit = {},
    ) : BasePref<Boolean>(key, defaultValue, onSet) {
        override fun read(): Boolean = sp.getBoolean(key, defaultValue)
        override fun write(editor: SharedPreferences.Editor, value: Boolean) {
            editor.putBoolean(key, value)
        }
    }

    inner class IntPref(
        key: String,
        defaultValue: Int = 0,
        onSet: () -> Unit = {},
    ) : BasePref<Int>(key, defaultValue, onSet) {
        override fun read(): Int = sp.getInt(key, defaultValue)
        override fun write(editor: SharedPreferences.Editor, value: Int) {
            editor.putInt(key, value)
        }
    }

    inner class FloatPref(
        key: String,
        defaultValue: Float = 0f,
        onSet: () -> Unit = {},
    ) : BasePref<Float>(key, defaultValue, onSet) {
        override fun read(): Float = sp.getFloat(key, defaultValue)
        override fun write(editor: SharedPreferences.Editor, value: Float) {
            editor.putFloat(key, value)
        }
    }

    inner class StringSetPref(
        key: String,
        defaultValue: Set<String> = emptySet(),
        onSet: () -> Unit = {},
    ) : BasePref<Set<String>>(key, defaultValue, onSet) {
        override fun read(): Set<String> = sp.getStringSet(key, defaultValue) ?: defaultValue
        override fun write(editor: SharedPreferences.Editor, value: Set<String>) {
            editor.putStringSet(key, value)
        }
    }

    inner class FontPref(
        key: String,
        defaultValue: FontCache.Font,
        onSet: () -> Unit = {},
    ) : BasePref<FontCache.Font>(key, defaultValue, onSet) {
        override fun read(): FontCache.Font {
            val string = sp.getString(key, null) ?: return defaultValue
            return FontCache.Font.fromString(string) ?: defaultValue
        }

        override fun write(editor: SharedPreferences.Editor, value: FontCache.Font) {
            editor.putString(key, value.toString())
        }
    }

    inner class IdpIntPref(
        key: String,
        private val defaultSelector: InvariantDeviceProfile.() -> Int,
        onSet: () -> Unit = {},
    ) : BasePref<Int>(key, 0, onSet) {
        override fun read(): Int = sp.getInt(key, defaultSelector(idp))
        override fun write(editor: SharedPreferences.Editor, value: Int) {
            editor.putInt(key, value)
        }
        fun get(gridOption: InvariantDeviceProfile.GridOption): Int {
            return sp.getInt(key, defaultSelector(gridOption.toInvariantDeviceProfile(context)))
        }
    }

    abstract inner class MutableMapPref<K, V>(
        key: String,
        onSet: () -> Unit = {},
    ) : BasePref<Map<K, V>>(key, emptyMap(), onSet) {

        override fun read(): Map<K, V> {
            val jsonString = sp.getString(key, null) ?: return emptyMap()
            val json = JSONObject(jsonString)
            val map = mutableMapOf<K, V>()
            json.keys().forEach {
                map[unflattenKey(it)] = unflattenValue(json.getString(it))
            }
            return map
        }

        override fun write(editor: SharedPreferences.Editor, value: Map<K, V>) {
            val json = JSONObject()
            value.forEach { (k, v) ->
                json.put(flattenKey(k), flattenValue(v))
            }
            editor.putString(key, json.toString())
        }

        operator fun set(key: K, value: V?) {
            val map = get().toMutableMap()
            if (value != null) {
                map[key] = value
            } else {
                map.remove(key)
            }
            set(map)
        }

        operator fun get(key: K): V? = get()[key]

        abstract fun flattenKey(key: K): String
        abstract fun unflattenKey(key: String): K
        abstract fun flattenValue(value: V): String
        abstract fun unflattenValue(value: String): V
    }

    protected fun migratePrefs(currentVersion: Int, migrate: (oldVersion: Int) -> Unit) {
        val oldVersion = sp.getInt("pref_version", 0)
        if (oldVersion < currentVersion) {
            migrate(oldVersion)
            sp.edit { putInt("pref_version", currentVersion) }
        }
    }
}
