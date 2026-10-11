package com.agcoding.networkapp.cloud.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException
import com.agcoding.networkapp.cloud.domain.model.SyncStage
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * "Your data isn't synced yet" flow shown over the app at launch: intro → account →
 * household → upload with progress → success. Skipping asks again in a week.
 */
@HiltViewModel
class SyncOnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CloudRepository,
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncOnboardingUiState())
    val uiState: StateFlow<SyncOnboardingUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var lastSyncReplacedLocal = false

    init {
        viewModelScope.launch {
            // Decided once: finishing or skipping the flow is what hides it
            val show = runCatching { repository.shouldPromptSync.first() }.getOrDefault(false)
            if (show) prefill()
            _uiState.update { it.copy(visible = show, decided = true) }
        }
    }

    fun onIntent(intent: SyncOnboardingIntent) {
        when (intent) {
            SyncOnboardingIntent.Start  -> start()
            SyncOnboardingIntent.Skip   -> {
                viewModelScope.launch { repository.snoozeSyncPrompt(SKIP_FOR_MILLIS) }
                _uiState.update { it.copy(visible = false) }
            }
            SyncOnboardingIntent.Back   -> _uiState.update { it.copy(step = SyncOnboardingStep.INTRO, message = null) }

            is SyncOnboardingIntent.SetSignUp      -> _uiState.update { it.copy(isSignUp = intent.signUp, message = null) }
            is SyncOnboardingIntent.UpdateName     -> _uiState.update { it.copy(nameInput = intent.value.take(100)) }
            is SyncOnboardingIntent.UpdateEmail    -> _uiState.update { it.copy(emailInput = intent.value.trim()) }
            is SyncOnboardingIntent.UpdatePassword -> _uiState.update { it.copy(passwordInput = intent.value) }
            SyncOnboardingIntent.SubmitAuth        -> submitAuth()

            is SyncOnboardingIntent.SetJoinWithCode     -> _uiState.update { it.copy(joinWithCode = intent.join, message = null) }
            is SyncOnboardingIntent.UpdateHouseholdName -> _uiState.update { it.copy(householdNameInput = intent.value.take(100)) }
            is SyncOnboardingIntent.UpdateInviteCode    -> _uiState.update {
                it.copy(inviteCodeInput = intent.value.uppercase().filter(Char::isLetterOrDigit).take(SyncOnboardingUiState.INVITE_LENGTH))
            }
            SyncOnboardingIntent.SubmitHousehold -> submitHousehold()

            SyncOnboardingIntent.Retry    -> upload(replaceLocal = lastSyncReplacedLocal)
            SyncOnboardingIntent.Continue -> _uiState.update { it.copy(visible = false) }
        }
    }

    private suspend fun prefill() {
        val profile = runCatching { getUserProfileUseCase().first() }.getOrNull()
        val name = profile?.name.orEmpty()
        _uiState.update {
            it.copy(
                nameInput          = name,
                emailInput         = profile?.email.orEmpty(),
                householdNameInput = if (name.isBlank()) context.getString(R.string.sync_onboarding_default_household)
                                     else context.getString(R.string.sync_onboarding_default_household_named, name),
            )
        }
    }

    /** "Yes": continue from wherever this phone already is (signed in, in a household…). */
    private fun start() {
        viewModelScope.launch {
            val cloud = repository.state.first()
            when {
                cloud.session == null   -> _uiState.update { it.copy(step = SyncOnboardingStep.ACCOUNT) }
                cloud.household == null -> _uiState.update { it.copy(step = SyncOnboardingStep.HOUSEHOLD) }
                else                    -> upload(replaceLocal = false)
            }
        }
    }

    private fun submitAuth() {
        val state = _uiState.value
        if (!state.canSubmitAuth) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val result = if (state.isSignUp) repository.signUp(state.emailInput, state.passwordInput, state.nameInput, syncAfter = false)
                         else repository.signIn(state.emailInput, state.passwordInput, syncAfter = false)
            result.fold(
                onSuccess = { hasHousehold -> afterSignIn(hasHousehold) },
                onFailure = ::showError,
            )
        }
    }

    private fun afterSignIn(hasHousehold: Boolean) {
        _uiState.update { it.copy(isBusy = false, passwordInput = "", message = null) }
        // Already in a household (e.g. set up on another phone): merge this phone's data into it
        if (hasHousehold) upload(replaceLocal = false)
        else _uiState.update { it.copy(step = SyncOnboardingStep.HOUSEHOLD) }
    }

    private fun submitHousehold() {
        val state = _uiState.value
        if (!state.canSubmitHousehold) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val result = if (state.joinWithCode) repository.joinHousehold(state.inviteCodeInput, syncAfter = false)
                         else repository.createHousehold(state.householdNameInput, syncAfter = false)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isBusy = false) }
                    upload(replaceLocal = state.joinWithCode)
                },
                onFailure = ::showError,
            )
        }
    }

    /** The upload screen: real progress from the sync, shown for at least a moment. */
    private fun upload(replaceLocal: Boolean) {
        lastSyncReplacedLocal = replaceLocal
        _uiState.update { it.copy(step = SyncOnboardingStep.SYNCING, stage = SyncStage.CONNECTING, progress = 0f, message = null) }

        val startedAt = System.currentTimeMillis()
        var realProgress = 0f
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            launch {
                repository.syncProgress
                    .filterNotNull()
                    .dropWhile { it.stage == SyncStage.DONE } // left over from an earlier sync
                    .collect { realProgress = maxOf(realProgress, it.fraction) }
            }
            // The bar follows the real progress but never faster than a short, readable pace,
            // so every message gets its moment even when the upload is instant
            while (true) {
                val pace = (System.currentTimeMillis() - startedAt).toFloat() / MIN_UPLOAD_SCREEN_MILLIS * 0.97f
                val shown = minOf(realProgress, pace)
                _uiState.update { it.copy(progress = maxOf(it.progress, shown), stage = stageFor(maxOf(it.progress, shown))) }
                delay(FRAME_MILLIS)
            }
        }

        viewModelScope.launch {
            val result = repository.syncWithSummary(replaceLocal)
            // Let the bar and the messages play out even when the sync is instant
            delay((MIN_UPLOAD_SCREEN_MILLIS - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0))
            progressJob?.cancel()
            result.fold(
                onSuccess = { summary ->
                    _uiState.update { it.copy(stage = SyncStage.DONE, progress = 1f) }
                    delay(DONE_PAUSE_MILLIS)
                    _uiState.update { it.copy(step = SyncOnboardingStep.SUCCESS, summary = summary) }
                },
                onFailure = { error ->
                    Timber.w(error, "Sync onboarding failed")
                    _uiState.update {
                        it.copy(step = SyncOnboardingStep.ERROR, message = error.messageRes(), messageIsError = true)
                    }
                },
            )
        }
    }

    /** The message that matches a point of the bar (same split as the sync's own stages). */
    private fun stageFor(fraction: Float): SyncStage = when {
        fraction < 0.08f -> SyncStage.CONNECTING
        fraction < 0.15f -> SyncStage.CHECKING
        fraction < 0.30f -> SyncStage.ACCOUNTS
        fraction < 0.75f -> SyncStage.ENTRIES
        fraction < 0.88f -> SyncStage.EXPENSES
        fraction < 0.95f -> SyncStage.CATEGORIES
        fraction < 1f    -> SyncStage.FINISHING
        else             -> SyncStage.DONE
    }

    private fun showError(error: Throwable) {
        Timber.w(error, "Sync onboarding step failed")
        _uiState.update { it.copy(isBusy = false, message = error.messageRes(), messageIsError = true) }
    }

    private fun Throwable.messageRes(): Int = when ((this as? CloudException)?.error ?: CloudError.UNKNOWN) {
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

    private companion object {
        const val SKIP_FOR_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val MIN_UPLOAD_SCREEN_MILLIS = 4_000L
        const val DONE_PAUSE_MILLIS = 700L
        const val FRAME_MILLIS = 40L
    }
}
