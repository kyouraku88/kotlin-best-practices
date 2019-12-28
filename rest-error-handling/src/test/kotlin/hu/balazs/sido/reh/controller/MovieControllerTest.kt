package hu.balazs.sido.reh.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.balazs.sido.reh.domain.Movie
import hu.balazs.sido.reh.model.RestErrorResponse
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val movieId = 1L
    private val movieIdNotFound = 100L

    @BeforeAll
    fun init() {
        RestAssured.port = port
    }

    @Test
    fun getMovieById_success() {
        val expectedResponse = Movie(1L,"First")

        When {
            get("/movies/$movieId")
        } Then {
            log().all()
            statusCode(200)
            body(Matchers.equalTo(objectMapper.writeValueAsString(expectedResponse)))
        }
    }

    @Test
    fun getMovieById_notFound() {
        val expectedResponse = RestErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Movie with 100 was not found."
        )

        When {
            get("/movies/$movieIdNotFound")
        } Then {
            log().all()
            statusCode(404)
            body(Matchers.equalTo(objectMapper.writeValueAsString(expectedResponse)))
        }
    }
}