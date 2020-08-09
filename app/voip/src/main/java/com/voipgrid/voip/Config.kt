package com.voipgrid.voip

import nl.spindle.phonelib.model.Codec

data class Config(
        val username: String,
        val password: String,
        val domain: String,
        val port: String
)