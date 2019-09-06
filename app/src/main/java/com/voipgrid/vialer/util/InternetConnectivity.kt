package com.voipgrid.vialer.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class InternetConnectivity {

    /**
     * Attempts to open a socket to guarantee we have an internet connection.
     *
     */
    suspend fun canGuaranteeAnInternetConnection(): Boolean = withContext(Dispatchers.IO) { attemptToOpenSocket() }

    /**
     * Perform the actual opening of a socket.
     *
     */
    private fun attemptToOpenSocket() = try {
        Socket().apply {
            connect(InetSocketAddress(IP, PORT), TIMEOUT)
            close()
        }
        true
    } catch (e: IOException) { false }

    companion object {

        /**
         * A sensible IP to open a socket to, this must be something we expect
         * to have incredibly high availability.
         *
         */
        private const val IP = "8.8.8.8"

        /**
         * The port to connect to at the given IP, for now this will be Google's DNS
         * server.
         *
         */
        private const val PORT = 53

        /**
         * The time to wait before we determine we don't have an internet connection.
         *
         */
        private const val TIMEOUT = 1500
    }
}