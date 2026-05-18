package com.agcoding.networkapp.shared.ui.utils

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class StringResource(
        @StringRes val id: Int,
        val args: Array<Any> = emptyArray(),
    ) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return id == other.id && args.contentEquals(other.args)
        }
        override fun hashCode() = 31 * id + args.contentHashCode()
    }

    data class DynamicString(val value: String) : UiText()
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.StringResource -> context.getString(id, *args)
    is UiText.DynamicString  -> value
}
