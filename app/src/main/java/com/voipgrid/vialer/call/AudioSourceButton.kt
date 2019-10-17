package com.voipgrid.vialer.call

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import com.voipgrid.vialer.R
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.audio.Routes
import kotlinx.android.synthetic.main.call_button.view.*
import org.pjsip.pjsua2.Call

class AudioSourceButton(context: Context, attrs: AttributeSet?) : CallActionButton(context, attrs), View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private lateinit var callback: (route: Routes) -> Unit
    lateinit var audio: AudioRouter

    init {
        setOnClickListener(this)
    }

    fun updateBasedOnAudioRouter() {
        if (!audio.isBluetoothRouteAvailable) {
            when (audio.isCurrentlyRoutingAudioViaSpeaker) {
                true -> activate()
                false -> deactivate()
            }

            updateTextAndImage(R.string.speaker_label, R.drawable.ic_speaker)
            return
        }

        when (audio.currentRoute) {
            Routes.BLUETOOTH -> updateTextAndImage(R.string.audio_source_option_bluetooth, R.drawable.audio_source_dropdown_bluetooth)
            Routes.SPEAKER -> updateTextAndImage(R.string.speaker_label, R.drawable.audio_source_dropdown_speaker)
            else -> updateTextAndImage(R.string.audio_source_option_phone, R.drawable.audio_source_dropdown_phone)
        }
    }

    override fun onClick(v: View?) {
        if (!audio.isBluetoothRouteAvailable) {
            callback.invoke(
                    if (audio.isCurrentlyRoutingAudioViaSpeaker) Routes.EARPIECE else Routes.SPEAKER
            )

            return
        }

        inflateAudioSourceMenu()
    }

    /**
     * Create a menu listing audio sources.
     *
     */
    private fun inflateAudioSourceMenu() {
        val popup = PopupMenu(context, this)
        popup.menuInflater.inflate(R.menu.menu_audio_source, popup.menu)
        audio.connectedBluetoothHeadset?.let {
            val menuItem = popup.menu.getItem(2)
            menuItem.title = menuItem.toString() + " (" + it.name + ")"
        }
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    /**
     * Handle when the user has clicked an item in the audio source menu.
     *
     */
    override fun onMenuItemClick(item: MenuItem): Boolean {
        callback.invoke(when (item.itemId) {
            R.id.audio_source_option_speaker -> Routes.SPEAKER
            R.id.audio_source_option_bluetooth -> Routes.BLUETOOTH
            else -> Routes.EARPIECE
        })

        return false
    }

    private fun updateTextAndImage(text: Int, imageResource: Int) {
        label.text = context.getText(text)
        image.setImageResource(imageResource)
    }

    fun setOnHandleAudioSourceSelectionListener(callback: (route: Routes) -> Unit) {
        this.callback = callback
    }
}