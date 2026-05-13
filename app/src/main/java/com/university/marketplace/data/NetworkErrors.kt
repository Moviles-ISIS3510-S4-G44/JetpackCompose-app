package com.university.marketplace.data

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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

fun Throwable.toUserFriendlyMessage(fallback: String = "Something went wrong. Please try again."): String {
    if (isNetworkConnectivityError()) {
        return "You appear to be offline. Please check your connection and try again."
    }
    val msg = message?.trim().orEmpty()
    return msg.ifEmpty { fallback }
}
