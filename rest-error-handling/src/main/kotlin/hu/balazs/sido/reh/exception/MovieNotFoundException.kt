package hu.balazs.sido.reh.exception

import java.lang.RuntimeException

class MovieNotFoundException(
        movieId: Long
): RuntimeException("Movie with [id=$movieId] was not found.")