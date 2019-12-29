package hu.balazs.sido.reh.controller

import com.nhaarman.mockitokotlin2.whenever
import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.exception.MovieNotFoundException
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieControllerTest {

    @LocalServerPort
    private var port: Int = 0

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
            log().all()
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(Movie::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

//    @Test
//    fun getMovieById_notFound() {
//        val expectedResponse = RestErrorResponse(
//                HttpStatus.NOT_FOUND.value(),
//                "Not found movie"
//        )
//
//        whenever(movieRepository.getMovieById(movieIdNotFound))
//                .thenThrow(MovieNotFoundException("Not found movie"))
//
//        val response = When {
//            get("/movies/$movieIdNotFound")
//        } Then {
//            log().all()
//            statusCode(HttpStatus.NOT_FOUND.value())
//        } Extract {
//            body().`as`(RestErrorResponse::class.java)
//        }
//
//        Assertions.assertEquals(expectedResponse, response)
//    }


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
            log().all()
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(Movie::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }

    @Test
    fun getMovieByTitle_notFound() {
        whenever(movieRepository.getMovieByTitle(movieTitle))
                .thenReturn(null)

        Given {
            param("title", movieTitle)
        } When {
            get("/movies")
        } Then {
            log().all()
        }
    }

}