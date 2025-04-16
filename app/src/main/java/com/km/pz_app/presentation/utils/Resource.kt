package com.km.pz_app.presentation.utils

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
