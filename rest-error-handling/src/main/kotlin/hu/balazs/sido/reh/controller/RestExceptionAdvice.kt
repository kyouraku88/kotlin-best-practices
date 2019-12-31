package hu.balazs.sido.reh.controller

import hu.balazs.sido.reh.common.getErrorCauses
import hu.balazs.sido.reh.exception.MovieNotFoundException
import hu.balazs.sido.reh.model.RestErrorCause
import hu.balazs.sido.reh.model.RestErrorResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class RestExceptionAdvice: ResponseEntityExceptionHandler() {

    @Value("\${rest.api.version}")
    private lateinit var restApiVersion: String

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MovieNotFoundException::class)
    fun handleMovieNotFound(ex: MovieNotFoundException) =
            RestErrorResponse(
                    apiVersion = restApiVersion,
                    status = HttpStatus.NOT_FOUND.value(),
                    message = ex.localizedMessage,
                    path = "/movies",
                    causes = ex.getErrorCauses()
            )

    override fun handleMethodArgumentNotValid(
            ex: MethodArgumentNotValidException,
            headers: HttpHeaders,
            status: HttpStatus,
            request: WebRequest): ResponseEntity<Any> {

        val response = RestErrorResponse(
                apiVersion = restApiVersion,
                status = status.value(),
                message = "Validation error while saving movie",
                path = "/movies",
                causes = ex.bindingResult.allErrors
                        .map {
                            RestErrorCause(
                                    ex::class.simpleName,
                                    it.defaultMessage
                            )
                        }
        )
        return ResponseEntity(response, status)
    }

}