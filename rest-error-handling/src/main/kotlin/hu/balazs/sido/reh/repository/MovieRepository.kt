package hu.balazs.sido.reh.repository

import hu.balazs.sido.reh.domain.Movie

interface MovieRepository {

    fun getMovieById(id: Long): Movie

    fun getMovieByTitle(title: String): Movie?

}