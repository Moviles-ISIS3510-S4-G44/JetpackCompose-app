package com.university.marketplace.ui.common

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is UnknownHostException,
        is ConnectException,
        is SocketTimeoutException -> "No hay conexion en este momento. Intenta nuevamente cuando tengas internet."
        is HttpException -> when (code()) {
            404 -> "No encontramos la informacion solicitada."
            401 -> "Tu sesion termino. Inicia sesion de nuevo para continuar."
            else -> "No pudimos completar la solicitud. Intenta nuevamente en unos minutos."
        }
        is IOException -> "No hay conexion en este momento. Intenta nuevamente cuando tengas internet."
        else -> "Algo salio mal. Intenta nuevamente."
    }
}

