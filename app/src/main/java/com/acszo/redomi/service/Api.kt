package com.acszo.redomi.service

import com.acszo.redomi.R
import okio.IOException
import retrofit2.Response
import java.net.SocketTimeoutException

object Api {

    const val BASE_URL_GITHUB = "https://api.github.com/repos/acszo/Redomi/"

}

sealed class ApiResult<T: Any> {
    data class Success<T: Any>(val data: T): ApiResult<T>()
    data class Error(val code: Int): ApiResult<Nothing>()
    data class Exception(val message: Int): ApiResult<Nothing>()
}

suspend fun <T: Any> apiCall(
    execute: suspend() -> Response<T>
): ApiResult<T> {
    return try {
        val response = execute()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body)
        } else {
            ApiResult.Error(response.code())
        }
    } catch(_: SocketTimeoutException) {
        ApiResult.Exception(R.string.error_timeout)
    } catch (_: IOException) {
        ApiResult.Exception(R.string.error_no_internet_connection)
    } catch (_: Exception) {
        ApiResult.Exception(R.string.error_generic)
    } as ApiResult<T>
}
