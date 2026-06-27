package id.andreasmlbngaol.identity.presentation.response

import kotlinx.serialization.Serializable

/**
 * Uniform response envelope used by every endpoint: { success, message, data }.
 * A single shape keeps client integration predictable across all services that
 * consume this identity provider.
 */
@Serializable
data class ApiResponse<out T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: ApiError? = null,
) {
    companion object {
        fun <T> ok(data: T?, message: String = "OK"): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun failure(message: String, error: ApiError?): ApiResponse<Unit> =
            ApiResponse(success = false, message = message, data = null, error = error)
    }
}

@Serializable
data class ApiError(
    val code: String,
    val details: Map<String, String>? = null,
)

@Serializable
data class PageMeta(
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Long,
)

@Serializable
data class PagedData<T>(
    val items: List<T>,
    val meta: PageMeta,
)
