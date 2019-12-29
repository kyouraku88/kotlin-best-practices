package hu.balazs.sido.reh.validation

import hu.balazs.sido.reh.domain.Movie
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

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