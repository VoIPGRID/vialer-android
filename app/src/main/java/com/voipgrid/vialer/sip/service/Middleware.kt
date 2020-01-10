package com.voipgrid.vialer.sip.service

import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.incoming.MiddlewareResponse

class Middleware(private val sip: SipService) {

    private val logger = Logger(this)

    private val pendingMiddlewareResponses = ArrayList<MiddlewareResponse>()

    fun add(middlewareResponse: MiddlewareResponse) = pendingMiddlewareResponses.add(middlewareResponse)

    /**
     * Respond to the middleware with the details for the incoming call.
     *
     */
    fun respond() {
        pendingMiddlewareResponses.forEach {
            logger.i("Responding with available for token ${it.token}")
            MiddlewareHelper.respond(it.token, it.startTime)
        }

        this.pendingMiddlewareResponses.clear()
    }
}