package com.agcoding.networkapp.cloud.domain.model

/** Why a cloud action failed, in terms the screen can explain. */
enum class CloudError {
    NO_CONNECTION,
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    USER_EXISTS,
    WEAK_PASSWORD,
    /** The new password is the same as the current one. */
    SAME_PASSWORD,
    INVALID_EMAIL,
    RATE_LIMITED,
    INVALID_INVITE,
    SESSION_EXPIRED,
    /** The temporary password email couldn't be sent. */
    EMAIL_SEND_FAILED,
    UNKNOWN,
}

class CloudException(val error: CloudError, cause: Throwable? = null) : Exception(error.name, cause)
