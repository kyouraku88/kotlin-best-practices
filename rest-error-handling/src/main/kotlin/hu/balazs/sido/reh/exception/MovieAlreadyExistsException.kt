package hu.balazs.sido.reh.exception

import java.lang.RuntimeException

class MovieAlreadyExistsException(
        movieTitle: String
): RuntimeException("Movie with [title=$movieTitle] already exists")