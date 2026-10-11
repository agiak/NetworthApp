package com.agcoding.networkapp.cloud.presentation.onboarding

import androidx.annotation.StringRes
import com.agcoding.networkapp.cloud.domain.model.SyncStage
import com.agcoding.networkapp.cloud.domain.model.SyncSummary

enum class SyncOnboardingStep { WELCOME, INTRO, ACCOUNT, HOUSEHOLD, SYNCING, SUCCESS, ERROR }

/** Where the first-launch welcome flow hands over to the rest of the setup. */
enum class WelcomeResult {
    /** Signed in to a household that has data: it's on the phone now, only security is left. */
    HOUSEHOLD_DATA,
    /** New account with a new household: continue with the target and the first accounts. */
    NEW_HOUSEHOLD,
    /** Use the app on this phone only: the usual profile setup. */
    NO_ACCOUNT,
}

data class SyncOnboardingUiState(
    /** Whether the flow covers the app; decided once per launch. */
    val visible: Boolean = false,
    /** False until it's known whether the data is synced. */
    val decided: Boolean = false,
    val step: SyncOnboardingStep = SyncOnboardingStep.INTRO,
    /** First launch: sign in or register before setting anything up. */
    val welcome: Boolean = false,
    /** Set once the welcome flow is done; tells the setup where to go next. */
    val welcomeResult: WelcomeResult? = null,

    // Account
    val isSignUp: Boolean = true,
    val nameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",

    // Household
    val joinWithCode: Boolean = false,
    val householdNameInput: String = "",
    val inviteCodeInput: String = "",

    // Upload (or, when [downloading], fetching the household's data)
    val downloading: Boolean = false,
    val stage: SyncStage = SyncStage.CONNECTING,
    val progress: Float = 0f,
    val summary: SyncSummary? = null,

    val isBusy: Boolean = false,
    @StringRes val message: Int? = null,
    val messageIsError: Boolean = false,
) {
    val canSubmitAuth: Boolean
        get() = !isBusy && emailInput.contains('@') && passwordInput.length >= MIN_PASSWORD &&
                (!isSignUp || nameInput.isNotBlank())

    val canSubmitHousehold: Boolean
        get() = !isBusy && if (joinWithCode) inviteCodeInput.length == INVITE_LENGTH else householdNameInput.isNotBlank()

    companion object {
        const val MIN_PASSWORD = 6
        const val INVITE_LENGTH = 8
    }
}

sealed interface SyncOnboardingIntent {
    data object BeginWelcome : SyncOnboardingIntent
    data object ChooseSignIn : SyncOnboardingIntent
    data object ChooseRegister : SyncOnboardingIntent
    data object ContinueWithoutAccount : SyncOnboardingIntent

    data object Start : SyncOnboardingIntent
    data object Skip : SyncOnboardingIntent
    data object Back : SyncOnboardingIntent

    data class SetSignUp(val signUp: Boolean) : SyncOnboardingIntent
    data class UpdateName(val value: String) : SyncOnboardingIntent
    data class UpdateEmail(val value: String) : SyncOnboardingIntent
    data class UpdatePassword(val value: String) : SyncOnboardingIntent
    data object SubmitAuth : SyncOnboardingIntent

    data class SetJoinWithCode(val join: Boolean) : SyncOnboardingIntent
    data class UpdateHouseholdName(val value: String) : SyncOnboardingIntent
    data class UpdateInviteCode(val value: String) : SyncOnboardingIntent
    data object SubmitHousehold : SyncOnboardingIntent

    data object Retry : SyncOnboardingIntent
    data object Continue : SyncOnboardingIntent
}
