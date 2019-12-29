package hu.balazs.sido.reh.validation

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MovieValidator::class])
annotation class MovieValid(val message: String = "Invalid parameter",
                            val groups: Array<KClass<*>> = [],
                            val payload: Array<KClass<out Payload>> = [])