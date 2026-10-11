package com.agcoding.networkapp.cloud.presentation

import androidx.annotation.StringRes
import com.agcoding.networkapp.cloud.domain.model.HouseholdMember

enum class CloudStep { SIGNED_OUT, NO_HOUSEHOLD, CONNECTED }

data class CloudUiState(
    val isLoading: Boolean = true,
    val step: CloudStep = CloudStep.SIGNED_OUT,
    val email: String = "",

    // Sign in / sign up
    val isSignUp: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",

    // Household
    val householdNameInput: String = "",
    val inviteCodeInput: String = "",
    val householdName: String = "",
    val isOwner: Boolean = false,
    val members: List<HouseholdMember> = emptyList(),
    val lastSyncAt: Long? = null,
    val isSyncing: Boolean = false,
    val syncFailed: Boolean = false,
    val isOnline: Boolean = true,
    val hasPendingChanges: Boolean = false,

    val isBusy: Boolean = false,
    /** A new invite code to show and share. */
    val inviteCode: String? = null,
    val confirmLeave: Boolean = false,
    @StringRes val message: Int? = null,
    val messageIsError: Boolean = false,
) {
    val canSubmitAuth: Boolean
        get() = !isBusy && emailInput.contains('@') && passwordInput.length >= MIN_PASSWORD &&
                (!isSignUp || nameInput.isNotBlank())

    companion object {
        const val MIN_PASSWORD = 6
    }
}
