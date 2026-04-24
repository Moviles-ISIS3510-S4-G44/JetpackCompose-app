package com.university.marketplace.ui.common

import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.UnauthorizedAuthException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object UserMessageMapper {
    fun fromError(throwable: Throwable, fallback: String): String {
        if (throwable is UnauthorizedAuthException) {
            return throwable.message ?: "Tu sesion expiro. Inicia sesion de nuevo."
        }

        if (throwable is AuthException) {
            return throwable.message ?: fallback
        }

        return when (throwable) {
            is UnknownHostException -> "No se pudo conectar al servidor. Revisa tu conexion a internet."
            is SocketTimeoutException -> "La solicitud tardo demasiado. Intenta nuevamente."
            is ConnectException -> "No se pudo establecer conexion con el servidor."
            is IOException -> "Hay un problema de red. Verifica tu conexion e intentalo otra vez."
            else -> fallback
        }
    }
}

