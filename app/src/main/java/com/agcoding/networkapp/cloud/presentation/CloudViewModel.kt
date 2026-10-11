package com.agcoding.networkapp.cloud.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException
import com.agcoding.networkapp.cloud.domain.model.CloudState
import com.agcoding.networkapp.cloud.domain.model.SignUpResult
import com.agcoding.networkapp.cloud.domain.model.SyncStatus
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudUiState())
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    private var loadedMembersFor: String? = null

    init {
        prefillFromProfile()
        observeCloud()
    }

    fun onIntent(intent: CloudIntent) {
        when (intent) {
            is CloudIntent.SetAuthMode        -> _uiState.update { it.copy(isSignUp = intent.signUp, message = null) }
            is CloudIntent.UpdateName         -> _uiState.update { it.copy(nameInput = intent.value.take(100)) }
            is CloudIntent.UpdateEmail        -> _uiState.update { it.copy(emailInput = intent.value.trim()) }
            is CloudIntent.UpdatePassword     -> _uiState.update { it.copy(passwordInput = intent.value) }
            CloudIntent.SubmitAuth            -> submitAuth()
            is CloudIntent.UpdateHouseholdName -> _uiState.update { it.copy(householdNameInput = intent.value.take(100)) }
            CloudIntent.CreateHousehold       -> busy { repository.createHousehold(_uiState.value.householdNameInput).map { R.string.cloud_msg_household_created } }
            is CloudIntent.UpdateInviteCode   -> _uiState.update { it.copy(inviteCodeInput = intent.value.uppercase().filter(Char::isLetterOrDigit).take(8)) }
            CloudIntent.JoinHousehold         -> busy { repository.joinHousehold(_uiState.value.inviteCodeInput).map { R.string.cloud_msg_joined } }
            CloudIntent.SyncNow               -> busy { repository.syncNow().map { R.string.cloud_msg_synced } }
            CloudIntent.CreateInvite          -> viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true) }
                repository.createInvite().fold(
                    onSuccess = { code -> _uiState.update { it.copy(isBusy = false, inviteCode = code) } },
                    onFailure = { error -> showError(error) },
                )
            }
            CloudIntent.DismissInvite         -> _uiState.update { it.copy(inviteCode = null) }
            CloudIntent.RequestLeave          -> _uiState.update { it.copy(confirmLeave = true) }
            CloudIntent.DismissLeave          -> _uiState.update { it.copy(confirmLeave = false) }
            CloudIntent.ConfirmLeave          -> {
                _uiState.update { it.copy(confirmLeave = false) }
                busy { repository.leaveHousehold().map { R.string.cloud_msg_left } }
            }
            CloudIntent.SignOut               -> viewModelScope.launch {
                repository.signOut()
                _uiState.update { it.copy(passwordInput = "", members = emptyList()) }
                loadedMembersFor = null
            }
            CloudIntent.ClearMessage          -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun prefillFromProfile() {
        viewModelScope.launch {
            val profile = runCatching { getUserProfileUseCase().first() }.getOrNull() ?: return@launch
            _uiState.update {
                it.copy(
                    nameInput  = it.nameInput.ifBlank { profile.name },
                    emailInput = it.emailInput.ifBlank { profile.email },
                )
            }
        }
    }

    private fun observeCloud() {
        viewModelScope.launch {
            combine(repository.state, repository.syncStatus, repository.isOnline, repository.hasPendingChanges) { cloud, status, online, pending ->
                CloudSnapshot(cloud, status, online, pending)
            }
                .collect { (cloud, status, online, pending) ->
                    val step = when {
                        cloud.session == null   -> CloudStep.SIGNED_OUT
                        cloud.household == null -> CloudStep.NO_HOUSEHOLD
                        else                    -> CloudStep.CONNECTED
                    }
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            step          = step,
                            email         = cloud.session?.email.orEmpty(),
                            householdName = cloud.household?.name.orEmpty(),
                            isOwner       = cloud.household?.isOwner == true,
                            lastSyncAt    = cloud.lastSyncAt,
                            isSyncing     = status == SyncStatus.Syncing,
                            syncFailed    = status is SyncStatus.Failed,
                            isOnline      = online,
                            hasPendingChanges = pending,
                        )
                    }
                    val householdId = cloud.household?.id
                    if (householdId != null && householdId != loadedMembersFor && status != SyncStatus.Syncing) {
                        loadedMembersFor = householdId
                        loadMembers()
                    }
                }
        }
    }

    private data class CloudSnapshot(
        val cloud: CloudState,
        val status: SyncStatus,
        val online: Boolean,
        val pending: Boolean,
    )

    private fun loadMembers() {
        viewModelScope.launch {
            repository.members()
                .onSuccess { members -> _uiState.update { it.copy(members = members) } }
                .onFailure { Timber.w(it, "Could not load household members") }
        }
    }

    private fun submitAuth() {
        val state = _uiState.value
        if (!state.canSubmitAuth) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val result = if (state.isSignUp) {
                repository.signUp(state.emailInput, state.passwordInput, state.nameInput).map { outcome ->
                    if (outcome == SignUpResult.ConfirmEmail) R.string.cloud_msg_confirm_email else R.string.cloud_msg_signed_in
                }
            } else {
                repository.signIn(state.emailInput, state.passwordInput).map { R.string.cloud_msg_signed_in }
            }
            result.fold(
                onSuccess = { message ->
                    _uiState.update {
                        it.copy(
                            isBusy = false, message = message, messageIsError = false,
                            passwordInput = if (message == R.string.cloud_msg_confirm_email) it.passwordInput else "",
                            // After confirming the email the user signs in with the same details
                            isSignUp = if (message == R.string.cloud_msg_confirm_email) false else it.isSignUp,
                        )
                    }
                },
                onFailure = { error -> showError(error) },
            )
        }
    }

    /** Runs a cloud action with the busy indicator and shows its outcome. */
    private fun busy(action: suspend () -> Result<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            action().fold(
                onSuccess = { message ->
                    _uiState.update { it.copy(isBusy = false, message = message, messageIsError = false, inviteCodeInput = "") }
                    loadedMembersFor = null
                    loadMembers()
                },
                onFailure = { error -> showError(error) },
            )
        }
    }

    private fun showError(error: Throwable) {
        Timber.w(error, "Cloud action failed")
        val cloudError = (error as? CloudException)?.error ?: CloudError.UNKNOWN
        _uiState.update { it.copy(isBusy = false, message = cloudError.messageRes(), messageIsError = true) }
    }

    private fun CloudError.messageRes(): Int = when (this) {
        CloudError.NO_CONNECTION       -> R.string.cloud_error_no_connection
        CloudError.INVALID_CREDENTIALS -> R.string.cloud_error_invalid_credentials
        CloudError.EMAIL_NOT_CONFIRMED -> R.string.cloud_error_email_not_confirmed
        CloudError.USER_EXISTS         -> R.string.cloud_error_user_exists
        CloudError.WEAK_PASSWORD       -> R.string.cloud_error_weak_password
        CloudError.INVALID_EMAIL       -> R.string.cloud_error_invalid_email
        CloudError.RATE_LIMITED        -> R.string.cloud_error_rate_limited
        CloudError.INVALID_INVITE      -> R.string.cloud_error_invalid_invite
        CloudError.SESSION_EXPIRED     -> R.string.cloud_error_session_expired
        CloudError.UNKNOWN             -> R.string.cloud_error_unknown
    }
}
