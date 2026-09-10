package com.kzwdaw.kyguukk.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 简单的偏好存储，持久化 UI 选择（性别、概览页模式）。
 * 使用 SharedPreferences，同样不被清理缓存影响。
 */
class PreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var silhouetteGender: String
        get() = prefs.getString(KEY_GENDER, "MALE") ?: "MALE"
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    var overviewMode: String
        get() = prefs.getString(KEY_OVERVIEW_MODE, "LIST") ?: "LIST"
        set(value) = prefs.edit().putString(KEY_OVERVIEW_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "fitness_prefs"
        private const val KEY_GENDER = "silhouette_gender"
        private const val KEY_OVERVIEW_MODE = "overview_mode"

        const val MODE_LIST = "LIST"
        const val MODE_SILHOUETTE = "SILHOUETTE"

        @Volatile private var instance: PreferencesRepository? = null
        fun get(context: Context): PreferencesRepository =
            instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context).also { instance = it }
            }
    }
}
