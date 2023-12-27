package com.carbidecowboy.intra.domain

sealed class OperationResult<out T> {
    data class Success<out T>(val data: T) : OperationResult<T>()
    data class Failure(val exception: Throwable? = null) : OperationResult<Nothing>()
}