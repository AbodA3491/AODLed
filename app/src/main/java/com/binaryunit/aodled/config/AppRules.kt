package com.binaryunit.aodled.config

import android.content.Context
import org.json.JSONObject

data class AppRule(
    val enabled: Boolean = false,
    val style: Style = Style.RING,
    val animation: Animation = Animation.OFF,
    val colour: Int = 0xFFFFFFFF.toInt(),
)

class AppRules(context: Context) {
    private val prefs = context.getSharedPreferences("aodled_app_rules", Context.MODE_PRIVATE)

    fun get(packageName: String): AppRule {
        val raw = prefs.getString(packageName, null) ?: return AppRule()
        return try {
            val j = JSONObject(raw)
            AppRule(
                enabled = j.optBoolean("enabled", false),
                style = enumValue(j.optString("style"), Style.RING),
                animation = enumValue(j.optString("animation"), Animation.OFF),
                colour = j.optInt("colour", 0xFFFFFFFF.toInt()),
            )
        } catch (_: Exception) { AppRule() }
    }

    fun set(packageName: String, rule: AppRule) {
        val j = JSONObject().put("enabled", rule.enabled).put("style", rule.style.name)
            .put("animation", rule.animation.name).put("colour", rule.colour)
        prefs.edit().putString(packageName, j.toString()).apply()
    }

    fun enabled(packageName: String): Boolean = get(packageName).enabled

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
