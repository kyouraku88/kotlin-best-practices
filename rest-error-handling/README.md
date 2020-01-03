
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
  - [3. ResponseStatusException](#3-responsestatusexception)
    - [3.1 Define a function that throws ResponseStatusException](#31-define-a-function-that-throws-responsestatusexception)
    - [3.2 Customise the response attributes](#32-customise-the-response-attributes)
      - [3.2.1 Basic customisation](#321-basic-customisation)
      - [3.2.2 Advanced customisation](#322-advanced-customisation)
    - [3.3 Testing](#33-testing)
  - [4. Validating @RequestBody](#4-validating-requestbody)
    - [4.1 Movie class validator](#41-movie-class-validator)
    - [4.2. Controller and Advice](#42-controller-and-advice)
    - [4.3. Testing](#43-testing)

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

Unit testing with rest assured MockMvc:

```kotlin
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
}
```

Integration testing with rest-assured kotlin extensions:

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
            statusCode(HttpStatus.NOT_FOUND.value())
        } Extract {
            body().`as`(RestErrorResponse::class.java)
        }

        Assertions.assertEquals(expectedResponse, response)
    }
}
```

The `@ExtendWith` annotation is how JUnit 5 extends the behavior of test classes or methods - previously in Junit 4 two types of test extensions were used: test runners and rules. JUnit 5 simplifies this by using the Extension API. On JUnit5 extensions read more [here](https://www.baeldung.com/junit-5-extensions) and on runwith [here](https://www.baeldung.com/junit-5-runwith).  
The `@SpringBootTest` annotation creates a web server that will be used for testing. In this mode the web environment will be created using a random port which is then injected into the field annotated with `@LocalServerPort`. This port is then used by `RestAssured`.  
The `@BeforeAll` annotation denotes a function that will be called only once, when the class is initialized, as marked by `@TestInstance`. (In Java this method would have to be static)

## 3. ResponseStatusException

The benefits:

- One type, multiple status codes: One exception type can lead to multiple different responses. This reduces tight coupling compared to the `@ExceptionHandler`
- We won't have to create as many custom exception classes
- More control over exception handling since the exceptions can be created programmatically

The tradeoffs:

- There's no unified way of exception handling: It's more difficult to enforce some application-wide conventions, as opposed to `@ControllerAdvice` which provides a global approach
- Code duplication: We may find ourselves replicating code in multiple controllers

We should also note that it's possible to combine different approaches within one application. For example, we can implement a @ControllerAdvice globally, but also ResponseStatusExceptions locally. However, we need to be careful: If the same exception can be handled in multiple ways, we may notice some surprising behavior. A possible convention is to handle one specific kind of exception always in one way.

### 3.1 Define a function that throws ResponseStatusException

We can create an instance of the exception by providing an `HttpStatus` and optionally a `reason`  and a `cause`:

```kotlin
/** The most simple implementation, don't need to specify the cause */
@GetMapping
fun getMovieByName(@RequestParam("title") title: String) =
        movieRepository.getMovieByTitle(title) ?:
            throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Movie with title $title was not found"
            )

/** Specify a message and set the caught exception */
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
```

### 3.2 Customise the response `attributes`

The error response is automatically generated by `BasicErrorController`. The default implementation of ErrorAttributes provides the following attributes when available:

- timestamp - The time that the errors were extracted
- status - The status code
- error - The error reason
- exception - The class name of the root exception (if configured)
- message - The exception message
- errors - Any ObjectErrors from a BindingResult exception
- trace - The exception stack trace
- path - The URL path when the exception was raised
- requestId - Unique ID associated with the current request

#### 3.2.1 Basic customisation

Before sending a response the error attributes can be intercepted and modified:

```kotlin
@Component
class RestErrorAttributes : DefaultErrorAttributes() {

    companion object {
        val dateFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    }

    override fun getErrorAttributes(
            webRequest: WebRequest?,
            includeStackTrace: Boolean): MutableMap<String, Any> {

        val errorAttributes =
                super.getErrorAttributes(webRequest, includeStackTrace)

        // formatted timestamp
        errorAttributes["timestamp"] = LocalDateTime.now().format(dateFormat)

        // add the cause message
        val rootError = getError(webRequest)
        rootError.cause?.let {
            val causeAttributes = mutableMapOf(
                    "exception" to it::class.simpleName,
                    "message" to it.message
            )
            errorAttributes["cause"] = causeAttributes
        }

        return errorAttributes
    }

}
```

Calling the method `getMovieByName` will produce a result:

```http
# fail to retrieve movie with title
GET http://localhost:8080/movies?title=Second
```

```json
{
    "timestamp": "05.12.2019 17:06:32",
    "status": 404,
    "error": "Not Found",
    "message": "Movie with [title=Second] was not found",
    "path": "/movies"
}
```

Calling the method `saveMovie` at least 2x will produce a result:

```http
# save a movie
POST http://localhost:8080/movies
Content-Type: application/json

{
  "title": "Movie title"
}
```

```json
{
  "timestamp": "05.12.2019 17:06:32",
  "status": 409,
  "error": "Conflict",
  "message": "Error while saving movie",
  "path": "/movies",
  "cause": {
    "exception": "MovieAlreadyExistsException",
    "message": "Movie with [title=Movie title] already exists"
  }
}
```

#### 3.2.2 Advanced customisation

Create a custom error object:

```kotlin
data class RestErrorResponse(
    val apiVersion: String,
    val status: Int,
    val message: String,
    val path: String,
    val causes: List<RestErrorCause> = emptyList()
) {
    fun toAttributeMap() =
        mutableMapOf(
            "apiVersion" to apiVersion,
            "status" to status,
            "message" to message,
            "path" to path,
            "causes" to causes
        )
}

data class RestErrorCause(
    val exception: String?,
    val message: String?
)
```

And rewrite the `RestErrorAttributes` to create the attributes using the `RestErrorResponse` class:

```kotlin
@Component
class RestErrorAttributes : DefaultErrorAttributes() {

    @Value("\${rest.api.version}")
    private lateinit var restApiVersion: String

    override fun getErrorAttributes(
            webRequest: WebRequest?,
            includeStackTrace: Boolean): MutableMap<String, Any> {
        val errorAttributes =
                super.getErrorAttributes(webRequest, includeStackTrace)

        // fill the errors
        val causes = mutableListOf<RestErrorCause>()
        var exception = getError(webRequest)
        while (exception?.cause != null && exception.cause != exception) {
            exception.cause?.let {
                causes.add(
                        RestErrorCause(
                                exception = it::class.simpleName,
                                message = it.message
                        )
                )
            }
            exception = exception.cause
        }

        val response = RestErrorResponse(
                apiVersion = restApiVersion,
                status = errorAttributes["status"] as Int,
                message = errorAttributes.getOrDefault(
                        "message",
                        "Error while performing request") as String,
                path = errorAttributes["path"] as String,
                causes = causes
        )
        return response.toAttributeMap()
    }
}
```

Calling the method `saveMovie` at least 2x will produce a result:

```http
# save a movie
POST http://localhost:8080/movies
Content-Type: application/json

{
  "title": "Movie title"
}
```

```json
{
  "apiVersion": "1.0",
  "status": 409,
  "message": "Error while saving movie",
  "path": "/movies",
  "causes": [
    {
      "exception": "MovieAlreadyExistsException",
      "message": "Movie with [title=Movie title] already exists"
    }
  ]
}
```

To tell spring to omit the stacktrace from the response, add the below line to the `application.properties` file:

```text
server.error.include-stacktrace = never
```

### 3.3 Testing

Unit testing:

```kotlin
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
```

Integration testing:

```kotlin
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
```

## 4. Validating @RequestBody

### 4.1 Movie class validator

```kotlin
class MovieValidator: ConstraintValidator<MovieValid, Movie> {

    override fun isValid(value: Movie, context: ConstraintValidatorContext): Boolean {
        var valid = true
        context.disableDefaultConstraintViolation()

        if (value.title.isBlank()) {
            valid = false
            context.buildConstraintViolationWithTemplate(
                    "Movie title can not be blank"
            ).addConstraintViolation()
        }

        return valid
    }
}
```

```kotlin
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MovieValidator::class])
annotation class MovieValid(val message: String = "Invalid parameter",
                            val groups: Array<KClass<*>> = [],
                            val payload: Array<KClass<out Payload>> = [])
```

Annotate the Movie class

```kotlin
@MovieValid
data class Movie(
    var id: Long?,
    val title: String
)
```

### 4.2. Controller and Advice

Add `@Validated` to the Controller class and add `@Valid` before the `@RequestBody`.

```kotlin
@Validated
@RestController
@RequestMapping("/movies")
class MovieController(
    val movieRepository: MovieRepository
) {
    @PostMapping
    fun saveMovie(@Valid @RequestBody movie: Movie) {
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
```

When the request body is not valid it will throw a `MethodArgumentNotValidException`. If the Controller extends `ResponseEntityExceptionHandler` don't create a new function with and annotate it as `@ExceptionHandler`, but override `handleMethodArgumentNotValid`.

```kotlin
@RestControllerAdvice
class RestExceptionAdvice: ResponseEntityExceptionHandler() {
    override fun handleMethodArgumentNotValid(
            ex: MethodArgumentNotValidException,
            headers: HttpHeaders,
            status: HttpStatus,
            request: WebRequest): ResponseEntity<Any> {

        val response = RestErrorResponse(
            apiVersion = restApiVersion,
            status = status.value(),
            message = "Error while loading movie",
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
```

### 4.3. Testing

```kotlin
@Test
fun saveMovie_notValid() {
    val toSave = Movie(1L, "   ")

    val expectedResponse = RestErrorResponse(
        restApiVersion,
        HttpStatus.BAD_REQUEST.value(),
        "Error while saving movie",
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
```

- [gradle with junit5](https://www.baeldung.com/junit-5-gradle)
- [rest-assured](https://github.com/rest-assured/rest-assured/wiki/gettingstarted)
- [kotlin junit 5 beforeall](https://stackoverflow.com/questions/38516418/what-is-proper-workaround-for-beforeall-in-kotlin)
- [koltin junit 5 rest assured connection refused](https://stackoverflow.com/questions/32054274/connection-refused-with-rest-assured-junit-test-case)
- [JUnit 5 extensions](https://www.baeldung.com/junit-5-extensions)
