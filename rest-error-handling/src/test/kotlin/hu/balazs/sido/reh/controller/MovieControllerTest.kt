package hu.balazs.sido.reh.controller

import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.exception.MovieAlreadyExistsException
import hu.balazs.sido.reh.exception.MovieNotFoundException
import hu.balazs.sido.reh.model.RestErrorCause
import hu.balazs.sido.reh.model.RestErrorResponse
import hu.balazs.sido.reh.repository.MovieRepository
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.module.mockmvc.RestAssuredMockMvc
import io.restassured.module.mockmvc.RestAssuredMockMvc.get
import io.restassured.module.mockmvc.RestAssuredMockMvc.given
import io.restassured.module.mockmvc.RestAssuredMockMvc.post
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.web.context.WebApplicationContext


@ExtendWith(SpringExtension::class)
@WebMvcTest(MovieController::class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieControllerTest {

    private companion object {
        const val BASE_URL = "movies"
    }

    @Autowired
    private lateinit var context: WebApplicationContext

    @Value("\${rest.api.version}")
    private lateinit var restApiVersion: String

    @MockBean
    private lateinit var movieRepository: MovieRepository

    private val movieId = 1L
    private val movieIdNotFound = 100L
    private val movieTitle = "Second"

    @BeforeAll
    fun init() {
        RestAssuredMockMvc.webAppContextSetup(context)
    }

    @Test
    fun getMovieById_success() {
        val expectedResponse = Movie(1L, "First")

        whenever(movieRepository.getMovieById(movieId))
                .thenReturn(expectedResponse)

        val response = get("$BASE_URL/$movieId")
        .then()
                .status(HttpStatus.OK)
        .extract()
                .body().`as`(Movie::class.java)

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

        val response = get("$BASE_URL/$movieIdNotFound")
        .then()
                .status(HttpStatus.NOT_FOUND)
        .extract()
                .body().`as`(RestErrorResponse::class.java)

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun getMovieByTitle_success() {
        val expectedResponse = Movie(2L, "Second")

        whenever(movieRepository.getMovieByTitle(movieTitle))
                .thenReturn(expectedResponse)

        val response = given()
                .param("title", movieTitle)
        .`when`()
                .get(BASE_URL)
        .then()
                .status(HttpStatus.OK)
        .extract()
                .body().`as`(Movie::class.java)

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun getMovieByTitle_notFound() {
        whenever(movieRepository.getMovieByTitle(movieTitle))
                .thenReturn(null)

        given()
                .param("title", movieTitle)
        .`when`()
                .get(BASE_URL)
        .then()
                .status(HttpStatus.NOT_FOUND)
    }

    @Test
    fun saveMovie_success() {
        val toSave = Movie(1L, "New movie")

        doNothing().whenever(movieRepository).saveMovie(toSave)

        given()
                .contentType(ContentType.JSON)
                .body(toSave)
        .`when`()
                .post(BASE_URL)
        .then()
                .status(HttpStatus.OK)
    }

    @Test
    fun saveMovie_conflict() {
        val toSave = Movie(1L, "New movie")

        whenever(movieRepository.saveMovie(toSave))
                .thenThrow(MovieAlreadyExistsException(toSave.title))

        given()
                .contentType(ContentType.JSON)
                .body(toSave)
        .`when`()
                .post(BASE_URL)
        .then()
                .status(HttpStatus.CONFLICT)
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

        val response = given()
                .contentType(ContentType.JSON)
                .body(toSave)
        .`when`()
                .post(BASE_URL)
        .then()
                .status(HttpStatus.BAD_REQUEST)
        .extract()
                .body().`as`(RestErrorResponse::class.java)

        Assertions.assertEquals(expectedResponse, response)
    }

}