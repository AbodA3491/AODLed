package com.binaryunit.aodled.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
            text = "Enable an app to let its notifications trigger the LED. Set its own colour, shape and animation."
            setPadding(20, 16, 20, 16)
        })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 4, 12, 24) }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        val apps = queryLauncherApps().sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        for (app in apps) {
            val pkg = app.packageName
            val current = rules.get(pkg)
            val card = MaterialCardView(this).apply {
                radius = 18f
                cardElevation = 0f
                setCardBackgroundColor(0xFF202124.toInt())
                val p = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
                list.addView(this, p)
            }
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 10, 14, 12)
            }
            card.addView(column)
            val check = CheckBox(this).apply {
                text = app.loadLabel(packageManager).toString()
                textSize = 16f
                isChecked = current.enabled
                setOnCheckedChangeListener { _, checked ->
                    rules.set(pkg, currentFor(pkg, checked, style, animation, colour))
                    style.isEnabled = checked; animation.isEnabled = checked; colour.isEnabled = checked
                }
            }
            column.addView(check)
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
            }
            column.addView(row)
            val style = spinner(row, Style.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, Style.entries.indexOf(current.style))
            val animation = spinner(row, Animation.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, Animation.entries.indexOf(current.animation))
            val colour = spinner(row, colours.map { it.first }, colours.indexOfFirst { it.second == current.colour }.coerceAtLeast(0))
            style.isEnabled = current.enabled; animation.isEnabled = current.enabled; colour.isEnabled = current.enabled
            style.onItemSelected { rules.set(pkg, currentFor(pkg, check.isChecked, Style.entries[it], Animation.entries[animation.selectedItemPosition], colours[colour.selectedItemPosition].second)) }
            animation.onItemSelected { rules.set(pkg, currentFor(pkg, check.isChecked, Style.entries[style.selectedItemPosition], Animation.entries[it], colours[colour.selectedItemPosition].second)) }
            colour.onItemSelected { rules.set(pkg, currentFor(pkg, check.isChecked, Style.entries[style.selectedItemPosition], Animation.entries[animation.selectedItemPosition], colours[it].second)) }
        }
    }

    private fun currentFor(pkg: String, enabled: Boolean, style: Spinner, animation: Spinner, colour: Spinner) =
        currentFor(pkg, enabled, Style.entries[style.selectedItemPosition], Animation.entries[animation.selectedItemPosition], colours[colour.selectedItemPosition].second)

    private fun currentFor(pkg: String, enabled: Boolean, style: Style, animation: Animation, colour: Int) =
        AppRule(enabled, style, animation, colour)

    private fun spinner(parent: LinearLayout, items: List<String>, selected: Int): Spinner {
        val s = Spinner(this)
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        s.setSelection(selected.coerceAtLeast(0))
        parent.addView(s, LinearLayout.LayoutParams(0, -wrapContent(), 1f))
        return s
    }

    private fun Spinner.onItemSelected(action: (Int) -> Unit) {
        onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = action(position)
        }
    }

    private fun wrapContent() = android.view.ViewGroup.LayoutParams.WRAP_CONTENT

    private fun queryLauncherApps(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = if (android.os.Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getInstalledApplications(0)
        }
        val launchable = packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
        return infos.filter { it.packageName != packageName && it.packageName in launchable }
    }
}
