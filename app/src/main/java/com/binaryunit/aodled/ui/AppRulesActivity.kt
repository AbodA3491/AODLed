package com.binaryunit.aodled.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.binaryunit.aodled.config.Animation
import com.binaryunit.aodled.config.AppRule
import com.binaryunit.aodled.config.AppRules
import com.binaryunit.aodled.config.Style
import com.binaryunit.aodled.service.AppState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class AppRulesActivity : AppCompatActivity() {
    private lateinit var rules: AppRules
    private val colours = listOf(
        "White" to Color.WHITE, "Red" to Color.RED, "Orange" to 0xFFFF9800.toInt(),
        "Yellow" to Color.YELLOW, "Green" to Color.GREEN, "Cyan" to Color.CYAN,
        "Blue" to Color.BLUE, "Purple" to 0xFF9C27B0.toInt(), "Pink" to 0xFFE91E63.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rules = AppRules(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(MaterialToolbar(this).apply {
            title = "Apps & individual lights"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        })
        root.addView(TextView(this).apply {
            text = "Choose which apps can trigger the notification light. Each enabled app has its own colour, shape and animation."
            setPadding(20, 16, 20, 16)
        })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 4, 12, 24) }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        queryLauncherApps().sortedBy { safeLabel(it).lowercase() }.forEach { app ->
            val pkg = app.packageName
            val initial = rules.get(pkg)
            val card = MaterialCardView(this).apply {
                radius = 18f
                cardElevation = 0f
                setCardBackgroundColor(0xFF202124.toInt())
                list.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
            }
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 10, 14, 12)
            }
            card.addView(column)
            val check = CheckBox(this).apply {
                text = safeLabel(app)
                textSize = 16f
                isChecked = initial.enabled
            }
            column.addView(check)
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            column.addView(row)
            val style = spinner(row, Style.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, Style.entries.indexOf(initial.style))
            val animation = spinner(row, Animation.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, Animation.entries.indexOf(initial.animation))
            val colour = spinner(row, colours.map { it.first }, colours.indexOfFirst { it.second == initial.colour }.coerceAtLeast(0))

            fun save() {
                val styleIndex = style.selectedItemPosition.coerceIn(0, Style.entries.lastIndex)
                val animationIndex = animation.selectedItemPosition.coerceIn(0, Animation.entries.lastIndex)
                val colourIndex = colour.selectedItemPosition.coerceIn(0, colours.lastIndex)
                rules.set(pkg, AppRule(check.isChecked, Style.entries[styleIndex],
                    Animation.entries[animationIndex], colours[colourIndex].second))
                // The listener reads the saved rules when the next notification arrives.
                // Do not force a service rebuild while this screen is being constructed.
                AppState.changed()
            }
            fun syncEnabled() {
                style.isEnabled = check.isChecked
                animation.isEnabled = check.isChecked
                colour.isEnabled = check.isChecked
            }
            check.setOnCheckedChangeListener { _, _ -> save(); syncEnabled() }
            style.onItemSelected { save() }
            animation.onItemSelected { save() }
            colour.onItemSelected { save() }
            syncEnabled()
        }
    }

    private fun safeLabel(app: ApplicationInfo): String = try {
        app.loadLabel(packageManager).toString().ifBlank { app.packageName }
    } catch (_: Exception) {
        app.packageName
    }

    private fun spinner(parent: LinearLayout, items: List<String>, selected: Int): Spinner {
        val s = Spinner(this)
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        parent.addView(s, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        s.setSelection(selected.coerceIn(0, items.lastIndex))
        return s
    }

    private fun Spinner.onItemSelected(action: () -> Unit) {
        var skipInitialSelection = true
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (skipInitialSelection) {
                    skipInitialSelection = false
                    return
                }
                action()
            }
        }
    }

    private fun queryLauncherApps(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchable = packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
        return launchable
    }
}
