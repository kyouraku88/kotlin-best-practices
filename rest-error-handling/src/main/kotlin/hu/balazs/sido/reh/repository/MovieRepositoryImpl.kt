package hu.balazs.sido.reh.repository

import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.exception.MovieNotFoundException
import org.springframework.stereotype.Component

@Component
class MovieRepositoryImpl: MovieRepository {

    companion object {
        val movies = mutableListOf(
                Movie(1, "First"),
                Movie(2, "Second")
        )
    }

    override fun getMovieById(id: Long) =
            movies.find { it.id == id } ?: throw MovieNotFoundException()

    override fun getMovieByTitle(title: String) =
            movies.find { it.title == title }

}