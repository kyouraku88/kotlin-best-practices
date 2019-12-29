package hu.balazs.sido.reh.domain

import hu.balazs.sido.reh.validation.MovieValid

@MovieValid
data class Movie(
        var id: Long?,
        val title: String
)