package com.prefixguard.app

import com.prefixguard.app.PrefixStore
import android.telecom.Call
import android.telecom.CallScreeningService

class SpamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {

        // Get the incoming caller number
        val number = callDetails.handle
            ?.schemeSpecificPart
            .orEmpty()

        // Keep digits only
        val digits = number.filter { it.isDigit() }

        // Normalize standard Indian phone number formats:
        // +91 79711 XXXXX -> 79711 XXXXX
        // 91 79711 XXXXX  -> 79711 XXXXX
        // 0 79711 XXXXX   -> 79711 XXXXX
        val normalizedNumber = when {
            digits.length == 12 &&
                    digits.startsWith("91") -> {
                digits.drop(2)
            }

            digits.length == 11 &&
                    digits.startsWith("0") -> {
                digits.drop(1)
            }

            else -> digits
        }

        // Read saved spam prefixes
        val prefixes = PrefixStore.get(this)

        // Block when the normalized number starts with a saved prefix
        val shouldBlock = normalizedNumber.isNotBlank() &&
                prefixes.any { savedPrefix ->
                    val prefix = savedPrefix.filter { it.isDigit() }

                    prefix.isNotEmpty() &&
                            normalizedNumber.startsWith(prefix)
                }

        // Tell Android whether to allow or reject the call
        val response = CallResponse.Builder()
            .setDisallowCall(shouldBlock)
            .setRejectCall(shouldBlock)
            .setSkipCallLog(shouldBlock)
            .setSkipNotification(shouldBlock)
            .build()

        respondToCall(callDetails, response)
    }
}