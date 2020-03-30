package com.voipgrid.vialer.warnings

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.voipgrid.vialer.R
import com.voipgrid.vialer.settings.SettingsActivity
import kotlinx.android.synthetic.main.view_warning.view.*

class Warning(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_warning, this)
        context.theme.obtainStyledAttributes(attrs, R.styleable.Warning, 0, 0).apply {
            message.text = getString(R.styleable.Warning_text)
            warning_icon.setImageDrawable(getDrawable(R.styleable.Warning_warning_icon))
            settings_link.visibility = if (getBoolean(R.styleable.Warning_show_link, true)) View.VISIBLE else View.GONE
            colorize(getInteger(R.styleable.Warning_priority, 0))
        }

        settings_link.setOnClickListener { context.startActivity(Intent(context, SettingsActivity::class.java)) }
    }

    /**
     * Color all elements based on the provided enum argument.
     *
     */
    private fun colorize(priority: Int) {
        val (background, foreground) = colorMap[priority] ?: throw IllegalArgumentException("Unable to find colours for this priority")

        container.background.setTint(context.getColor(background))
        warning_icon.setColorFilter(context.getColor(foreground))
        message.setTextColor(context.getColor(foreground))
        settings_link.setTextColor(context.getColor(foreground))
    }

    companion object {

        /**
         * Map the attribute enum to a foreground and background color.
         *
         */
        private val colorMap = mapOf(
                0 to arrayOf(R.color.error_high_background_color, R.color.error_high_foreground_color),
                1 to arrayOf(R.color.error_medium_background_color, R.color.error_medium_foreground_color),
                2 to arrayOf(R.color.error_low_background_color, R.color.error_low_foreground_color)
        )
    }
}