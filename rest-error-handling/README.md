
# REST: Handling errors

- [REST: Handling errors](#rest-handling-errors)
  - [1. Basic setup, default error handling](#1-basic-setup-default-error-handling)
    - [1.1 Entity](#11-entity)
    - [1.2 Repository implementation](#12-repository-implementation)
    - [1.3 Controller](#13-controller)
    - [1.4 Manual testing](#14-manual-testing)

The application demonstrates the best practices when handling REST errors in a kotlin spring application.  
This document is a step-by-step tutorial how the application was developed and tested.

## 1. Basic setup, `default error handling`

- create an entity
- create a repository
- create a rest controller
- test the application

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

Testing an error:

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
