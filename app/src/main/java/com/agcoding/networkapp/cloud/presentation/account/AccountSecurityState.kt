package com.agcoding.networkapp.cloud.presentation.account

import androidx.annotation.StringRes

enum class AccountDialog { CHANGE_PASSWORD, FORGOT_PASSWORD, SIGN_OUT, DELETE_ACCOUNT }

data class AccountSecurityUiState(
    /** The signed-in cloud account, or null when signed out. */
    val email: String? = null,
    /** Signed in with a temporary password: a new one must be chosen. */
    val mustChangePassword: Boolean = false,
    val dialog: AccountDialog? = null,

    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmPasswordInput: String = "",
    val forgotEmailInput: String = "",
    /** The temporary password email went out (or would have, for an unknown email). */
    val forgotSent: Boolean = false,
    /** Signing out couldn't upload everything first; confirming again signs out anyway. */
    val signOutNeedsForce: Boolean = false,

    val isBusy: Boolean = false,
    @StringRes val message: Int? = null,
    val messageIsError: Boolean = false,
) {
    val isSignedIn: Boolean get() = email != null

    val passwordsMatch: Boolean get() = newPasswordInput == confirmPasswordInput

    /** The forced change after a temporary password doesn't ask for the current one. */
    val canChangePassword: Boolean
        get() = !isBusy && newPasswordInput.length >= MIN_PASSWORD && passwordsMatch &&
                (mustChangePassword || currentPasswordInput.isNotEmpty())

    val canSendForgot: Boolean get() = !isBusy && forgotEmailInput.contains('@')

    val canDeleteAccount: Boolean get() = !isBusy && currentPasswordInput.isNotEmpty()

    companion object {
        const val MIN_PASSWORD = 6
    }
}

sealed interface AccountSecurityIntent {
    data class Open(val dialog: AccountDialog, val email: String = "") : AccountSecurityIntent
    data object Dismiss : AccountSecurityIntent

    data class UpdateCurrentPassword(val value: String) : AccountSecurityIntent
    data class UpdateNewPassword(val value: String) : AccountSecurityIntent
    data class UpdateConfirmPassword(val value: String) : AccountSecurityIntent
    data class UpdateForgotEmail(val value: String) : AccountSecurityIntent

    data object SubmitChangePassword : AccountSecurityIntent
    data object SubmitForgotPassword : AccountSecurityIntent
    data object ConfirmSignOut : AccountSecurityIntent
    data object ConfirmDeleteAccount : AccountSecurityIntent
}
