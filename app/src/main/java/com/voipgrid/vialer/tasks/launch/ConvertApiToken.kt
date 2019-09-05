package com.voipgrid.vialer.tasks.launch

import android.preference.PreferenceManager
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.cryptography.Encrypter

/**
 * We have to decrypt the token and store it in the new format. This can be removed at a later
 * date.
 *
 */
class ConvertApiToken : OnLaunchTask {

    override fun execute(application: VialerApplication) {
        val encrypter = Encrypter(application)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        if (!sharedPreferences.contains("TOKEN_KEY")) return
        val encryptedValue = sharedPreferences.getString("TOKEN_KEY", null) ?: return
        User.loginToken = encrypter.decrypt(encryptedValue)
        sharedPreferences.edit().remove("TOKEN_KEY").apply()
    }
}