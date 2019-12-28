package hu.balazs.sido.reh.exception

import java.lang.RuntimeException

class MovieNotFoundException(override val message: String): RuntimeException(message)