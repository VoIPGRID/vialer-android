package com.voipgrid.vialer.onboarding.core

/**
 * A class that is used to maintain state across the onboarding process.
 *
 */
class OnboardingState {
    var username: String = ""
    var password = ""
    var requiresTwoFactor = false
    var hasVoipAccount = true
}