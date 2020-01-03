package hu.balazs.sido.reh.controller

import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.exception.MovieAlreadyExistsException
import hu.balazs.sido.reh.exception.MovieNotFoundException
import hu.balazs.sido.reh.model.RestErrorCause
import hu.balazs.sido.reh.model.RestErrorResponse
import hu.balazs.sido.reh.repository.MovieRepository
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Value("\${rest.api.version}")
    private lateinit var restApiVersion: String

    @MockBean
    private lateinit var movieRepository: MovieRepository

    private val movieId = 1L
    private val movieIdNotFound = 100L
    private val movieTitle = "Second"

    @BeforeAll
    fun init() {
        RestAssured.port = port
    }

    @Test
    fun getMovieById_success() {
        val expectedResponse = Movie(1L, "First")

        whenever(movieRepository.getMovieById(movieId))
                .thenReturn(expectedResponse)

        val response = When {
            get("/movies/$movieId")
        } Then {
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(Movie::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun getMovieById_notFound() {
        val expectedResponse = RestErrorResponse(
                restApiVersion,
                HttpStatus.NOT_FOUND.value(),
                "Movie with [id=$movieIdNotFound] was not found.",
                "/movies",
                emptyList()
        )

        whenever(movieRepository.getMovieById(movieIdNotFound))
                .thenThrow(MovieNotFoundException(movieIdNotFound))

        val response = When {
            get("/movies/$movieIdNotFound")
        } Then {
            statusCode(HttpStatus.NOT_FOUND.value())
        } Extract {
            body().`as`(RestErrorResponse::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }


    @Test
    fun getMovieByTitle_success() {
        val expectedResponse = Movie(2L, "Second")

        whenever(movieRepository.getMovieByTitle(movieTitle))
                .thenReturn(expectedResponse)

        val response = Given {
            param("title", movieTitle)
        } When {
            get("/movies")
        } Then {
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(Movie::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun getMovieByTitle_notFound() {
        val expectedResponse = RestErrorResponse(
                restApiVersion,
                HttpStatus.NOT_FOUND.value(),
                "Movie with title Second was not found",
                "/movies",
                emptyList()
        )

        whenever(movieRepository.getMovieByTitle(movieTitle))
                .thenReturn(null)

        val response = Given {
            param("title", movieTitle)
        } When {
            get("/movies")
        } Then {
            statusCode(HttpStatus.NOT_FOUND.value())
        } Extract {
            body().`as`(RestErrorResponse::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun saveMovie_success() {
        val toSave = Movie(1L, "New movie")

        doNothing().whenever(movieRepository).saveMovie(toSave)

        Given {
            contentType(MediaType.APPLICATION_JSON_VALUE)
            body(toSave)
        } When {
            post("/movies")
        } Then {
            statusCode(HttpStatus.OK.value())
        }
    }

    @Test
    fun saveMovie_conflict() {
        val toSave = Movie(1L, "New movie")

        val expectedResponse = RestErrorResponse(
                restApiVersion,
                HttpStatus.CONFLICT.value(),
                "Error while saving movie",
                "/movies",
                listOf(
                        RestErrorCause(
                                "MovieAlreadyExistsException",
                                "Movie with [title=${toSave.title}] already exists"
                        )
                )
        )

        whenever(movieRepository.saveMovie(toSave))
                .thenThrow(MovieAlreadyExistsException(toSave.title))

        val response = Given {
            contentType(MediaType.APPLICATION_JSON_VALUE)
            body(toSave)
        } When {
            post("/movies")
        } Then {
            statusCode(HttpStatus.CONFLICT.value())
        } Extract {
            body().`as`(RestErrorResponse::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun saveMovie_notValid() {
        val toSave = Movie(null, title = "   ")

        val expectedResponse = RestErrorResponse(
                restApiVersion,
                HttpStatus.BAD_REQUEST.value(),
                "Validation error while saving movie",
                "/movies",
                listOf(
                        RestErrorCause(
                                "MethodArgumentNotValidException",
                                "Movie title can not be blank"
                        )
                )
        )

        whenever(movieRepository.saveMovie(toSave))
                .thenThrow(MovieAlreadyExistsException(toSave.title))

        val response = Given {
            contentType(MediaType.APPLICATION_JSON_VALUE)
            body(toSave)
        } When {
            post("/movies")
        } Then {
            statusCode(HttpStatus.BAD_REQUEST.value())
        } Extract {
            body().`as`(RestErrorResponse::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

}