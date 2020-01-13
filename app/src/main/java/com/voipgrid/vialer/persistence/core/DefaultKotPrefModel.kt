package com.voipgrid.vialer.persistence.core

import com.chibatching.kotpref.KotprefModel

/**
 * Abstract class that will use the default shared settings when
 * creating a new KotprefModel.
 *
 */
abstract class DefaultKotPrefModel : KotprefModel() {
    override val kotprefName: String = "${context.applicationContext.packageName}_preferences"
}