package hu.balazs.sido.reh.model

data class RestErrorResponse(
        val apiVersion: String,
        val status: Int,
        val message: String,
        val path: String,
        val causes: List<RestErrorCause> = emptyList()
) {
    fun toAttributeMap() =
            mutableMapOf(
                    "apiVersion" to apiVersion,
                    "status" to status,
                    "message" to message,
                    "path" to path,
                    "causes" to causes
            )
}

data class RestErrorCause(
        val exception: String?,
        val message: String?
)