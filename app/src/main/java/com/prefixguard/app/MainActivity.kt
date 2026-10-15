package com.prefixguard.app

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val blue = Color.rgb(49, 87, 213)
    private val ink = Color.rgb(27, 34, 52)
    private val muted = Color.rgb(102, 112, 133)
    private val page = Color.rgb(247, 248, 252)
    private lateinit var status: TextView
    private lateinit var prefixList: LinearLayout
    private lateinit var prefixInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = page
        if (Build.VERSION.SDK_INT >= 26) window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
            setPadding(dp(22), dp(28), dp(22), dp(18))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(label("PREFIX GUARD", 12, blue, true).apply { letterSpacing = 0.12f })
        content.addView(label("Stop spam calls\nbefore they ring.", 29, ink, true).apply {
            setPadding(0, dp(12), 0, dp(8))
        })
        content.addView(label("Block incoming calls by their starting digits. Your list stays on this phone.", 15, muted, false))

        val statusCard = card().apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        status = label("Checking call-screening permission…", 15, ink, true)
        statusCard.addView(status)
        val roleButton = button("Enable call screening") { requestScreeningRole() }
        val roleParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) }
        statusCard.addView(roleButton, roleParams)
        val statusParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) }
        content.addView(statusCard, statusParams)

        content.addView(label("Your blocked prefixes", 20, ink, true).apply {
            setPadding(0, dp(26), 0, dp(6))
        })
        content.addView(label("Example: 140 or 98765. Any number starting with a saved prefix will be rejected.", 14, muted, false))

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        prefixInput = EditText(this).apply {
            hint = "Enter starting digits"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 15f
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(Color.WHITE, Color.rgb(222, 226, 235), 12)
        }
        addRow.addView(prefixInput, LinearLayout.LayoutParams(0, dp(50), 1f))
        val addButton = button("Add") { addPrefix() }
        addRow.addView(addButton, LinearLayout.LayoutParams(dp(76), dp(50)).apply { leftMargin = dp(8) })
        content.addView(addRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        prefixInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addPrefix(); true } else false
        }

        prefixList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(prefixList, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content.addView(label("Important: Android must keep Prefix Guard selected as the call-screening app. Calls from hidden/private numbers cannot be matched to a digit prefix.", 12, muted, false).apply {
            setPadding(0, dp(22), 0, dp(14))
        })

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderPrefixes()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else false
        status.text = if (enabled) "✓ Call screening is enabled" else "Call screening is not enabled yet"
        status.setTextColor(if (enabled) Color.rgb(24, 125, 75) else ink)
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 101)
            } else Toast.makeText(this, "Call screening is not available on this phone.", Toast.LENGTH_LONG).show()
        } else Toast.makeText(this, "Android 10 or newer is required.", Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated by Android API; still supported for role result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) updateStatus()
    }

    private fun addPrefix() {
        val prefix = prefixInput.text.toString().trim().filter { it.isDigit() }
        if (prefix.length < 3) {
            prefixInput.error = "Enter at least 3 digits"
            return
        }
        val updated = PrefixStore.get(this).toMutableSet()
        if (!updated.add(prefix)) {
            Toast.makeText(this, "That prefix is already added", Toast.LENGTH_SHORT).show()
        } else {
            PrefixStore.save(this, updated)
            prefixInput.setText("")
            hideKeyboard()
            renderPrefixes()
            Toast.makeText(this, "Prefix added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderPrefixes() {
        if (!::prefixList.isInitialized) return
        prefixList.removeAllViews()
        val prefixes = PrefixStore.get(this).sorted()
        if (prefixes.isEmpty()) {
            prefixList.addView(label("No prefixes added yet.", 14, muted, false).apply {
                setPadding(dp(4), dp(14), dp(4), dp(14))
            })
            return
        }
        prefixes.forEach { prefix ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.WHITE, Color.rgb(232, 235, 242), 12)
                setPadding(dp(14), dp(5), dp(8), dp(5))
            }
            val number = label(prefix, 17, ink, true)
            row.addView(number, LinearLayout.LayoutParams(0, dp(48), 1f))
            val remove = Button(this).apply {
                text = "Remove"
                textSize = 12f
                isAllCaps = false
                setTextColor(Color.rgb(177, 47, 56))
                background = rounded(Color.rgb(255, 241, 242), Color.TRANSPARENT, 10)
                setOnClickListener {
                    val updated = PrefixStore.get(this@MainActivity).toMutableSet()
                    updated.remove(prefix)
                    PrefixStore.save(this@MainActivity, updated)
                    renderPrefixes()
                }
            }
            row.addView(remove, LinearLayout.LayoutParams(dp(82), dp(40)))
            prefixList.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(prefixInput.windowToken, 0)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.WHITE, Color.rgb(232, 235, 242), 16)
    }

    private fun label(textValue: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = textValue
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun button(textValue: String, action: () -> Unit) = Button(this).apply {
        text = textValue
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(blue, blue, 12)
        setOnClickListener { action() }
    }

    private fun rounded(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
