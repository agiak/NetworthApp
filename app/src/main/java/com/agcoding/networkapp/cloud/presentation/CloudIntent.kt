package com.agcoding.networkapp.cloud.presentation

sealed interface CloudIntent {
    data class SetAuthMode(val signUp: Boolean) : CloudIntent
    data class UpdateName(val value: String) : CloudIntent
    data class UpdateEmail(val value: String) : CloudIntent
    data class UpdatePassword(val value: String) : CloudIntent
    data object SubmitAuth : CloudIntent

    data class UpdateHouseholdName(val value: String) : CloudIntent
    data object CreateHousehold : CloudIntent
    data class UpdateInviteCode(val value: String) : CloudIntent
    data object JoinHousehold : CloudIntent

    data object SyncNow : CloudIntent
    data object CreateInvite : CloudIntent
    data object DismissInvite : CloudIntent
    data object RequestLeave : CloudIntent
    data object ConfirmLeave : CloudIntent
    data object DismissLeave : CloudIntent
    data object ClearMessage : CloudIntent
}
