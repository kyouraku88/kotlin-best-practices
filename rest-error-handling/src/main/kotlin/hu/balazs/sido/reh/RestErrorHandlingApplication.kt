package hu.balazs.sido.reh

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RestErrorHandlingApplication

fun main(args: Array<String>) {
	runApplication<RestErrorHandlingApplication>(*args)
}
