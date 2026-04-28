package com.org.commcons

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.org.commcons.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("sevalink_prefs", Context.MODE_PRIVATE)

        binding.btnBack.setOnClickListener { finish() }

        // Set current state of toggle
        val isDark = prefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDark
        binding.tvModeLabel.text = if (isDark) "Dark Mode" else "Light Mode"

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            binding.tvModeLabel.text = if (isChecked) "Dark Mode" else "Light Mode"

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(
                this,
                if (isChecked) "Dark mode enabled 🌙" else "Light mode enabled ☀️",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        fun applyTheme(context: Context) {
            val prefs = context.getSharedPreferences("sevalink_prefs", Context.MODE_PRIVATE)
            val isDark = prefs.getBoolean("dark_mode", false)
            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}