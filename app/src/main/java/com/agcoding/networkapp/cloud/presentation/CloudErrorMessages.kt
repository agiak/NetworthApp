package com.agcoding.networkapp.cloud.presentation

import androidx.annotation.StringRes
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException

/** The message that explains why a cloud action failed. */
@StringRes
internal fun Throwable.cloudMessageRes(): Int = ((this as? CloudException)?.error ?: CloudError.UNKNOWN).messageRes()

@StringRes
internal fun CloudError.messageRes(): Int = when (this) {
    CloudError.NO_CONNECTION       -> R.string.cloud_error_no_connection
    CloudError.INVALID_CREDENTIALS -> R.string.cloud_error_invalid_credentials
    CloudError.EMAIL_NOT_CONFIRMED -> R.string.cloud_error_email_not_confirmed
    CloudError.USER_EXISTS         -> R.string.cloud_error_user_exists
    CloudError.WEAK_PASSWORD       -> R.string.cloud_error_weak_password
    CloudError.SAME_PASSWORD       -> R.string.cloud_error_same_password
    CloudError.INVALID_EMAIL       -> R.string.cloud_error_invalid_email
    CloudError.RATE_LIMITED        -> R.string.cloud_error_rate_limited
    CloudError.INVALID_INVITE      -> R.string.cloud_error_invalid_invite
    CloudError.SESSION_EXPIRED     -> R.string.cloud_error_session_expired
    CloudError.EMAIL_SEND_FAILED   -> R.string.cloud_error_email_send_failed
    CloudError.UNSYNCED_CHANGES    -> R.string.cloud_error_unsynced_changes
    CloudError.UNKNOWN             -> R.string.cloud_error_unknown
}
