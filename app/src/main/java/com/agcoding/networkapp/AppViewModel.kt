package com.agcoding.networkapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.IsProfileCreatedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val getAppThemeUseCase: GetAppThemeUseCase,
    private val isProfileCreatedUseCase: IsProfileCreatedUseCase
) : ViewModel() {

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _isProfileCreated = MutableStateFlow<Boolean?>(null)
    val isProfileCreated: StateFlow<Boolean?> = _isProfileCreated.asStateFlow()

    init {
        viewModelScope.launch {
            getAppThemeUseCase().collect { _appTheme.value = it }
        }
        viewModelScope.launch {
            isProfileCreatedUseCase().collect { _isProfileCreated.value = it }
        }
    }
}
