package com.voipgrid.vialer

import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText

/**
 * Listens for a click on the drawable on the right hand side of the edit text
 * and executes the callback.
 *
 */
fun EditText.setRightDrawableOnClickListener(callback: () -> Unit) {
    setOnTouchListener { _: View, event: MotionEvent -> Boolean
        val right = 2

        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= (this.right - compoundDrawables[right].bounds.width())) {
                callback()
                return@setOnTouchListener true
            }
        }

        return@setOnTouchListener false
    }
}

/**
 * Set an onClick listener but will automatically disable the button after it has been clicked once.
 *
 */
fun Button.setOnClickListenerAndDisable(function: (View) -> Unit) {
    setOnClickListener {
        it.isEnabled = false
        function.invoke(it)
    }
}

/**
 * Set an onClick listener but will automatically disable the button after it has been clicked once.
 *
 */
fun Button.setOnClickListenerAndDisable(listener: View.OnClickListener) {
    setOnClickListener {
        it.isEnabled = false
        listener.onClick(it)
    }
}