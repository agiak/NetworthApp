package com.agcoding.networkapp.biometric.presentation

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt

/**
 * Biometrics the app unlocks with: fingerprint and face.
 *
 * Most phones rate face unlock as a weak (class 2) biometric — Samsung, Xiaomi, Pixels before
 * the Pixel 8 — so asking only for strong biometrics left it out and allowed fingerprint alone.
 * The lock only gates the app (nothing is decrypted with a key), so weak biometrics are enough.
 */
object BiometricAuth {

    const val AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    /** Whether the device has fingerprint or face unlock set up. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun promptInfo(title: String, subtitle: String, negativeButtonText: String): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            // Face unlock opens the app as soon as the face is recognised, without an extra tap
            .setConfirmationRequired(false)
            .build()
}
