package com.voipgrid.vialer.call

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import androidx.core.graphics.drawable.DrawableCompat
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.call_button.view.*

open class CallActionButton(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val drawable: Drawable?
    private val originalDrawable: Drawable
    private val originalText: String

    init {
        inflate(context, R.layout.call_button, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CallActionButton)
        label.text = attributes.getString(R.styleable.CallActionButton_label)
        image.setImageDrawable(attributes.getDrawable(R.styleable.CallActionButton_image))
        drawable = attributes.getDrawable(R.styleable.CallActionButton_image)
        attributes.recycle()
        originalDrawable = image.drawable
        originalText = label.text.toString()
    }

    fun enable() {
        this.isActivated = true
        this.alpha = ENABLED_ALPHA
    }

    fun disable() {
        this.isActivated = false
        this.alpha = DISABLED_ALPHA
    }

    fun activate() {
        image.setColorFilter(Color.WHITE)
        image.background.setColorFilter(resources.getColor(R.color.color_primary), PorterDuff.Mode.SRC_ATOP)
        label.setTextColor(resources.getColor(R.color.color_primary))
    }

    fun deactivate() {
        image.clearColorFilter()
        image.background.clearColorFilter()
        label.setTextColor(resources.getColor(R.color.call_color))
    }

    fun swapIconAndText(replacementImage: Int, replacementText: Int) {
        image.setImageResource(replacementImage)
        label.text = context.getString(replacementText)
    }

    fun resetIconAndText() {
        if (image.drawable != originalDrawable) {
            image.setImageDrawable(originalDrawable)
        }

        label.text = originalText
    }

    companion object {

        const val ENABLED_ALPHA = 1.0f
        const val DISABLED_ALPHA = 0.5f
    }

    fun enable(enable: Boolean): Unit = when (enable) {
        true -> this.enable()
        false -> this.disable()
    }


    fun activate(activate: Boolean): Unit = when (activate) {
        true -> this.activate()
        false -> this.deactivate()
    }
}
