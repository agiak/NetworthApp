package com.agcoding.networkapp.cloud.presentation.account

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException
import com.agcoding.networkapp.cloud.domain.usecase.SignOutUseCase
import com.agcoding.networkapp.cloud.presentation.cloudMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/** Change password, forgot password, sign out and delete account for the cloud account. */
@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSecurityUiState())
    val uiState: StateFlow<AccountSecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { cloud ->
                _uiState.update {
                    it.copy(email = cloud.session?.email, mustChangePassword = cloud.session?.mustChangePassword == true)
                }
            }
        }
    }

    fun onIntent(intent: AccountSecurityIntent) {
        when (intent) {
            is AccountSecurityIntent.Open -> _uiState.update {
                it.copy(
                    dialog = intent.dialog,
                    currentPasswordInput = "", newPasswordInput = "", confirmPasswordInput = "",
                    forgotEmailInput = intent.email.ifBlank { it.email.orEmpty() },
                    forgotSent = false, signOutNeedsForce = false, message = null,
                )
            }
            AccountSecurityIntent.Dismiss -> _uiState.update {
                if (it.isBusy) it else it.copy(dialog = null, currentPasswordInput = "", newPasswordInput = "", confirmPasswordInput = "", message = null)
            }

            is AccountSecurityIntent.UpdateCurrentPassword -> _uiState.update { it.copy(currentPasswordInput = intent.value, message = null) }
            is AccountSecurityIntent.UpdateNewPassword     -> _uiState.update { it.copy(newPasswordInput = intent.value, message = null) }
            is AccountSecurityIntent.UpdateConfirmPassword -> _uiState.update { it.copy(confirmPasswordInput = intent.value, message = null) }
            is AccountSecurityIntent.UpdateForgotEmail     -> _uiState.update { it.copy(forgotEmailInput = intent.value.trim(), message = null) }

            AccountSecurityIntent.SubmitChangePassword -> changePassword()
            AccountSecurityIntent.SubmitForgotPassword -> forgotPassword()
            AccountSecurityIntent.ConfirmSignOut       -> signOut()
            AccountSecurityIntent.ConfirmDeleteAccount -> deleteAccount()
        }
    }

    private fun changePassword() {
        val state = _uiState.value
        if (!state.canChangePassword) return
        busy {
            repository.changePassword(
                currentPassword = state.currentPasswordInput.takeUnless { state.mustChangePassword },
                newPassword = state.newPasswordInput,
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isBusy = false, dialog = null,
                            currentPasswordInput = "", newPasswordInput = "", confirmPasswordInput = "",
                            message = R.string.account_password_changed, messageIsError = false,
                        )
                    }
                },
                onFailure = ::showError,
            )
        }
    }

    private fun forgotPassword() {
        val state = _uiState.value
        if (!state.canSendForgot) return
        busy {
            repository.requestTempPassword(state.forgotEmailInput, emailLanguage()).fold(
                onSuccess = { _uiState.update { it.copy(isBusy = false, forgotSent = true) } },
                onFailure = ::showError,
            )
        }
    }

    private fun signOut() {
        // Asked again after a failed upload: sign out anyway
        val force = _uiState.value.signOutNeedsForce
        busy {
            signOutUseCase(force).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isBusy = false, dialog = null, signOutNeedsForce = false, message = R.string.account_signed_out, messageIsError = false)
                    }
                },
                onFailure = { error ->
                    showError(error)
                    if ((error as? CloudException)?.error == CloudError.UNSYNCED_CHANGES) {
                        _uiState.update { it.copy(signOutNeedsForce = true) }
                    }
                },
            )
        }
    }

    private fun deleteAccount() {
        val state = _uiState.value
        if (!state.canDeleteAccount) return
        busy {
            repository.deleteAccount(state.currentPasswordInput).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isBusy = false, dialog = null, currentPasswordInput = "", message = R.string.account_deleted, messageIsError = false)
                    }
                },
                onFailure = ::showError,
            )
        }
    }

    private fun busy(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            action()
        }
    }

    private fun showError(error: Throwable) {
        Timber.w(error, "Account action failed")
        _uiState.update { it.copy(isBusy = false, message = error.cloudMessageRes(), messageIsError = true) }
    }

    /** The email goes out in the app's language. */
    private fun emailLanguage(): String {
        val language = AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
        return if (language == "el") "el" else "en"
    }
}
