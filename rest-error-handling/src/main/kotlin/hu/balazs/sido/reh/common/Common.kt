package hu.balazs.sido.reh.common

import hu.balazs.sido.reh.model.RestErrorCause

fun Throwable.getErrorCauses(): MutableList<RestErrorCause> {
    val causes = mutableListOf<RestErrorCause>()
    var exception: Throwable? = this
    while (exception?.cause != null && exception.cause != exception) {
        exception.cause?.let {
            causes.add(
                    RestErrorCause(
                            exception = it::class.simpleName,
                            message = it.message
                    )
            )
        }
        exception = exception.cause
    }
    return causes
}
