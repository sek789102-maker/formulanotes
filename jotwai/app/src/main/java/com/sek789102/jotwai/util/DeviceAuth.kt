package com.sek789102.jotwai.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * Asks for the phone's own screen lock (fingerprint, face, PIN, pattern or password).
 * Locked notes are only hidden behind this check; their text is not encrypted.
 */
object DeviceAuth {

    fun isAvailable(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isDeviceSecure
    }

    /**
     * Android 10+: system prompt. Android 7–9: the older confirm-credential screen, which needs
     * an activity result — [legacyLauncher] receives that intent and must call back.
     */
    fun authenticate(
        activity: Activity,
        title: String,
        legacyLauncher: (android.content.Intent) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        if (!isAvailable(activity)) {
            onResult(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = BiometricPrompt.Builder(activity).setTitle(title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
            val prompt = builder.build()
            prompt.authenticate(
                CancellationSignal(),
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onResult(true)
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onResult(false)
                }
            )
        } else {
            val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            @Suppress("DEPRECATION")
            val intent = km.createConfirmDeviceCredentialIntent(title, null)
            if (intent == null) onResult(true) else legacyLauncher(intent)
        }
    }
}
