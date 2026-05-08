package com.agcoding.networkapp.backup.domain.model

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile

data class AppBackupData(
    val version: Int,
    val exportedAt: String,
    val profile: UserProfile?,
    val theme: AppTheme?,
    val language: AppLanguage?,
    val entries: List<NetWorthEntry>
)
