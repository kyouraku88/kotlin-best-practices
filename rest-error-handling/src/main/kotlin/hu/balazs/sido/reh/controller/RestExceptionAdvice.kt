package hu.balazs.sido.reh.controller

import hu.balazs.sido.reh.model.RestErrorResponse
import hu.balazs.sido.reh.exception.MovieNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class RestExceptionAdvice: ResponseEntityExceptionHandler() {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MovieNotFoundException::class)
    fun handleMovieNotFound(ex: MovieNotFoundException) =
            RestErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message)

}