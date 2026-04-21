package com.university.marketplace.data

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Returns true when [throwable] looks like a connectivity failure raised by the HTTP stack
 * (OkHttp / Retrofit / the JDK networking layer).
 */
fun Throwable.isNetworkConnectivityError(): Boolean = when (this) {
    is UnknownHostException,
    is ConnectException,
    is SocketTimeoutException -> true
    is IOException -> {
        val msg = message.orEmpty()
        msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Software caused connection abort", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true)
    }
    else -> false
}

/**
 * Converts a thrown exception into a message suitable for display in the UI.
 * Network failures are collapsed into a single user-friendly line instead of
 * leaking stack-trace-style text like "Failed to connect to /10.0.2.2:8000".
 */
fun Throwable.toUserFriendlyMessage(fallback: String = "Something went wrong. Please try again."): String {
    if (isNetworkConnectivityError()) {
        return "You appear to be offline. Please check your connection and try again."
    }
    val msg = message?.trim().orEmpty()
    return msg.ifEmpty { fallback }
}
