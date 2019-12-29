package hu.balazs.sido.reh.controller

import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.repository.MovieRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/movies")
class MovieController(
    val movieRepository: MovieRepository
) {

    @GetMapping("/{id}")
    fun getMovieById(@PathVariable id: Long) =
            movieRepository.getMovieById(id)

    @GetMapping
    fun getMovieByName(@RequestParam("title") title: String) =
            movieRepository.getMovieByTitle(title) ?:
                throw ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movie with title $title was not found"
                )

    @PostMapping
    fun saveMovie(@RequestBody movie: Movie) {
        try {
            movieRepository.saveMovie(movie)
        } catch (ex: Exception) {
            throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Error while saving movie",
                    ex
            )
        }
    }
}