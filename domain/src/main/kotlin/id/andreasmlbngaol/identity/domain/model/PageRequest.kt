package id.andreasmlbngaol.identity.domain.model

/** Simple offset-based pagination request used by admin/list queries. */
data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val search: String? = null,
) {
    val offset: Long get() = page.toLong() * size.toLong()
    val limit: Int get() = size.coerceIn(1, MAX_PAGE_SIZE)

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}

/** A page of results plus the total count for building pagination metadata. */
data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
) {
    val totalPages: Int get() = if (size == 0) 0 else ((total + size - 1) / size).toInt()
}
