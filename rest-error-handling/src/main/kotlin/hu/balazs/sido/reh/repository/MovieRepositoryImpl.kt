package hu.balazs.sido.reh.repository

import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.exception.MovieNotFoundException
import org.springframework.stereotype.Component

@Component
class MovieRepositoryImpl: MovieRepository {

    val movies = mutableListOf<Movie>()

    @Throws(MovieNotFoundException::class)
    override fun getMovieById(id: Long) =
            movies.find { it.id == id } ?: throw MovieNotFoundException("Movie with $id was not found.")

    override fun getMovieByTitle(title: String) =
            movies.find { it.title == title }

}