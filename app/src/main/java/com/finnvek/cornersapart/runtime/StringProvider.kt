package com.finnvek.cornersapart.runtime

import androidx.annotation.StringRes

fun interface StringProvider {
    fun getString(
        @StringRes resourceId: Int,
    ): String
}
