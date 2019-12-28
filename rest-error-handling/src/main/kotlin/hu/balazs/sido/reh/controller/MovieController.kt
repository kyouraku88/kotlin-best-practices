package hu.balazs.sido.reh.controller

import hu.balazs.sido.reh.repository.MovieRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
            movieRepository.getMovieByTitle(title)

}