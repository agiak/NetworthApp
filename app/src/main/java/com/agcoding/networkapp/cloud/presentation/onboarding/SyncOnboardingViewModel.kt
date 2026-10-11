package com.agcoding.networkapp.cloud.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.cloud.domain.model.SyncStage
import com.agcoding.networkapp.cloud.presentation.cloudMessageRes
import com.agcoding.networkapp.settings.domain.model.UserProfile
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetProfileCreatedUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetUserProfileUseCase
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
import java.time.LocalDate
import javax.inject.Inject

/**
 * "Your data isn't synced yet" flow shown over the app at launch: intro → account →
 * household → upload with progress → success. Skipping asks again in a week.
 *
 * On first launch it's the welcome flow instead ([SyncOnboardingIntent.BeginWelcome]): sign in
 * (and bring the household's data to this phone) or register, before the rest of the setup.
 */
@HiltViewModel
class SyncOnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CloudRepository,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val setUserProfileUseCase: SetUserProfileUseCase,
    private val setProfileCreatedUseCase: SetProfileCreatedUseCase,
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
            SyncOnboardingIntent.BeginWelcome -> _uiState.update {
                if (it.welcome) it else it.copy(welcome = true, step = SyncOnboardingStep.WELCOME)
            }
            SyncOnboardingIntent.ChooseSignIn   -> _uiState.update { it.copy(step = SyncOnboardingStep.ACCOUNT, isSignUp = false, message = null) }
            SyncOnboardingIntent.ChooseRegister -> _uiState.update { it.copy(step = SyncOnboardingStep.ACCOUNT, isSignUp = true, message = null) }
            SyncOnboardingIntent.ContinueWithoutAccount -> {
                viewModelScope.launch { repository.snoozeSyncPrompt(SKIP_FOR_MILLIS) }
                _uiState.update { it.copy(welcomeResult = WelcomeResult.NO_ACCOUNT) }
            }

            SyncOnboardingIntent.Start  -> start()
            SyncOnboardingIntent.Skip   -> {
                if (_uiState.value.welcome) {
                    // Signed in already: the household's data comes with the next sync
                    finishWelcome(WelcomeResult.HOUSEHOLD_DATA)
                } else {
                    viewModelScope.launch { repository.snoozeSyncPrompt(SKIP_FOR_MILLIS) }
                    _uiState.update { it.copy(visible = false) }
                }
            }
            SyncOnboardingIntent.Back   -> _uiState.update {
                it.copy(step = if (it.welcome) SyncOnboardingStep.WELCOME else SyncOnboardingStep.INTRO, message = null)
            }

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
            SyncOnboardingIntent.Continue -> {
                if (_uiState.value.welcome) finishWelcome(WelcomeResult.HOUSEHOLD_DATA)
                else _uiState.update { it.copy(visible = false) }
            }
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
        _uiState.update {
            val defaultName = context.getString(R.string.sync_onboarding_default_household)
            it.copy(
                isBusy = false, passwordInput = "", message = null,
                // Name the new household after the person who just registered
                householdNameInput = if (it.isSignUp && it.nameInput.isNotBlank() && it.householdNameInput.let { n -> n.isBlank() || n == defaultName })
                    context.getString(R.string.sync_onboarding_default_household_named, it.nameInput.trim())
                else it.householdNameInput,
            )
        }
        when {
            // First launch: nothing on this phone yet, so take the household's data
            hasHousehold && _uiState.value.welcome -> upload(replaceLocal = true)
            // Already in a household (e.g. set up on another phone): merge this phone's data into it
            hasHousehold -> upload(replaceLocal = false)
            else -> _uiState.update { it.copy(step = SyncOnboardingStep.HOUSEHOLD) }
        }
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
                    // A new household on first launch has nothing to upload yet: go on with the setup
                    if (state.welcome && !state.joinWithCode) finishWelcome(WelcomeResult.NEW_HOUSEHOLD)
                    else upload(replaceLocal = state.joinWithCode)
                },
                onFailure = ::showError,
            )
        }
    }

    /** The upload screen: real progress from the sync, shown for at least a moment. */
    private fun upload(replaceLocal: Boolean) {
        lastSyncReplacedLocal = replaceLocal
        _uiState.update {
            it.copy(
                step = SyncOnboardingStep.SYNCING, stage = SyncStage.CONNECTING, progress = 0f, message = null,
                downloading = replaceLocal,
            )
        }

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
                        it.copy(step = SyncOnboardingStep.ERROR, message = error.cloudMessageRes(), messageIsError = true)
                    }
                },
            )
        }
    }

    /**
     * Ends the welcome flow: this phone's profile takes the account's name and email, so the
     * profile step can be skipped.
     */
    private fun finishWelcome(result: WelcomeResult) {
        val state = _uiState.value
        if (state.isBusy || state.welcomeResult != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val cloudEmail = repository.state.first().session?.email.orEmpty()
            val email = cloudEmail.ifBlank { state.emailInput }
            val name = state.nameInput.takeIf { state.isSignUp && it.isNotBlank() }
                ?: repository.displayName()
                ?: email.substringBefore('@')
            runCatching {
                val current = getUserProfileUseCase().first()
                setUserProfileUseCase(
                    UserProfile(
                        name         = name.trim(),
                        email        = email,
                        targetAmount = current.targetAmount,
                        createdAt    = current.createdAt ?: LocalDate.now(),
                    )
                )
                // A new household still sets its target and accounts; that step marks the profile done
                if (result == WelcomeResult.HOUSEHOLD_DATA) setProfileCreatedUseCase(true)
            }.onFailure { Timber.e(it, "Could not save the profile after signing in") }
            _uiState.update { it.copy(isBusy = false, welcomeResult = result) }
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
        _uiState.update { it.copy(isBusy = false, message = error.cloudMessageRes(), messageIsError = true) }
    }

    private companion object {
        const val SKIP_FOR_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val MIN_UPLOAD_SCREEN_MILLIS = 4_000L
        const val DONE_PAUSE_MILLIS = 700L
        const val FRAME_MILLIS = 40L
    }
}
