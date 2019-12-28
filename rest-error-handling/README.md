
# REST: Handling errors

- [REST: Handling errors](#rest-handling-errors)
  - [1. Basic setup, default error handling](#1-basic-setup-default-error-handling)
    - [1.1 Entity](#11-entity)
    - [1.2 Repository implementation](#12-repository-implementation)
    - [1.3 Controller](#13-controller)
    - [1.4 Manual testing](#14-manual-testing)
  - [2. ControllerAdvice](#2-controlleradvice)
    - [2.1 Create a custom response class and update the exception](#21-create-a-custom-response-class-and-update-the-exception)
    - [2.2 Change the repository throwing the exception](#22-change-the-repository-throwing-the-exception)
    - [2.3 Create a class annotated with @ControllerAdvice](#23-create-a-class-annotated-with-controlleradvice)
    - [2.4 Manual testing](#24-manual-testing)
    - [2.5 Testing with junit 5 and rest-assured](#25-testing-with-junit-5-and-rest-assured)

The application demonstrates the best practices when handling REST errors in a kotlin spring application.  
This document is a step-by-step tutorial how the application was developed and tested.

## 1. Basic setup, `default error handling`

### 1.1 Entity

```kotlin
data class Movie(
    val id: Long,
    val title: String
)

class MovieNotFoundException: Throwable()
```

### 1.2 Repository implementation

```kotlin
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
```

### 1.3 Controller

```kotlin
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
```

### 1.4 Manual testing

Testing success:

```http
# successfully retrieve movie with id
GET http://localhost:8080/movies/1

###

# successfully retrieve movie with title
GET http://localhost:8080/movies?title=First
```

Calling these methods will produce the result:

```json
{
  "id": 1,
  "title": "First"
}
```

Try to get a movie that does not exist in the repo:

```http
# fail to retrieve movie with id
GET http://localhost:8080/movies/100
```

Will result in an error response:

```json
{
  "timestamp": "2019-12-28T12:52:59.598+0000",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Invocation failure\nController [hu.balazs.sido.reh.controller.MovieController]\nMethod [public hu.balazs.sido.reh.domain.Movie hu.balazs.sido.reh.controller.MovieController.getMovieById(long)] with argument values:\n [0] [type=java.lang.Long] [value=100] ",
  "path": "/movies/100"
}
```

## 2. ControllerAdvice

The `@ControllerAdvice` annotation allows us to consolidate our multiple, scattered `@ExceptionHandlers` from before into a single, global error handling component.

The actual mechanism is extremely simple but also very flexible. It gives us:

- Full control over the body of the response as well as the status code
- Mapping of several exceptions to the same method, to be handled together
- It makes good use of the newer RESTful ResposeEntity response

### 2.1 Create a custom `response` class and update the `exception`

The exception has to extend `RuntimeExtension`, otherwise throwing this extension by a mock will result in: `Checked exception is invalid for this method!` error. This is because kotlin does not have checked exceptions, but the test nonetheless is expeting it to be declared.

```kotlin
data class RestErrorResponse(
        val status: Int,
        val message: String
)

class MovieNotFoundException(
        override val message: String
): RuntimeException(message)
```

### 2.2 Change the repository throwing the exception

```kotlin
@Component
class MovieRepositoryImpl: MovieRepository {
    override fun getMovieById(id: Long) =
            movies.find { it.id == id } ?:
              throw MovieNotFoundException("Movie with $id was not found.")

    // the rest of the class is unchanged
}
```

### 2.3 Create a class annotated with `@ControllerAdvice`

```kotlin
@ControllerAdvice
class RestExceptionHandler: ResponseEntityExceptionHandler {
    @ExceptionHandler(MovieNotFoundException.class)
    fun handleNotFound(ex: Exception, request: WebRequest) {

        val errorResponse = RestErrorResponse(
          HttpStatus.NOT_FOUND.value(),
          ex.message
        )

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }
}
```

Creating the `ResponseEntity` object is unnecessary when the function is annotated with `@ResponseBody` and `@ResponseStatus`

```kotlin
@ControllerAdvice
class RestExceptionHandler: ResponseEntityExceptionHandler {
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MovieNotFoundException.class)
    fun handleMovieNotFound(ex: Exception, request: WebRequest) =
        RestErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message)
}
```

This can be further simplified with the `@RestControllerAdvice` which is a combination of `@ControllerAdvice` with `@ResponseBody` automatically added to all the methods annotated with `@ExceptionHandler`.

```kotlin
@RestControllerAdvice
class RestExceptionAdvice: ResponseEntityExceptionHandler() {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MovieNotFoundException::class)
    fun handleMovieNotFound(ex: MovieNotFoundException) =
            RestErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message)
}
```

### 2.4 Manual testing

Testing an error:

```http
# fail to retrieve movie with id
GET http://localhost:8080/movies/100
```

Will result in an error response:

```json
{
  "status": 404,
  "message": "Movie with 100 was not found."
}
```

### 2.5 Testing with junit 5 and rest-assured

Add the following dependencies to gradle.build:

```gradle
dependencies {
  testImplementation("io.rest-assured:rest-assured:4.1.2")
  testImplementation("io.rest-assured:spring-mock-mvc:4.1.2")
  testImplementation("io.rest-assured:json-path:4.1.2")
  testImplementation("io.rest-assured:xml-path:4.1.2")
  testImplementation("io.rest-assured:json-schema-validator:4.1.2")
  testImplementation("io.rest-assured:kotlin-extensions:4.1.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}
```

Create a test class:

```kotlin
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

    @Test
    fun getMovieById_notFound() {
        val expectedResponse = RestErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not found movie"
        )

        whenever(movieRepository.getMovieById(movieIdNotFound))
                .thenThrow(MovieNotFoundException("Not found movie"))

        val response = When {
            get("/movies/$movieIdNotFound")
        } Then {
            log().all()
            statusCode(HttpStatus.NOT_FOUND.value())
        } Extract {
            body().`as`(RestErrorResponse::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }
```

The `@ExtendWith` annotation is how JUnit 5 extends the behavior of test classes or methods - previously in Junit 4 two types of test extensions were used: test runners and rules. JUnit 5 simplifies this by using the Extension API. On JUnit5 extensions read more [here](https://www.baeldung.com/junit-5-extensions) and on runwith [here](https://www.baeldung.com/junit-5-runwith).  
The `@SpringBootTest` annotation creates a web server that will be used for testing. In this mode the web environment will be created using a random port which is then injected into the field annotated with `@LocalServerPort`. This port is then used by `RestAssured`.  
The `@BeforeAll` annotation denotes a function that will be called only once, when the class is initialized, as marked by `@TestInstance`. (In Java this method would have to be static)

- [gradle with junit5](https://www.baeldung.com/junit-5-gradle)
- [rest-assured](https://github.com/rest-assured/rest-assured/wiki/gettingstarted)
- [kotlin junit 5 beforeall](https://stackoverflow.com/questions/38516418/what-is-proper-workaround-for-beforeall-in-kotlin)
- [koltin junit 5 rest assured connection refused](https://stackoverflow.com/questions/32054274/connection-refused-with-rest-assured-junit-test-case)
- [JUnit 5 extensions](https://www.baeldung.com/junit-5-extensions)
