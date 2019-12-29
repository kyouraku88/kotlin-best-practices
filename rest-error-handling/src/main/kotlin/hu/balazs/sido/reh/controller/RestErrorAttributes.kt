package hu.balazs.sido.reh.controller

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class RestErrorAttributes : DefaultErrorAttributes() {

    companion object {
        val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    }

    override fun getErrorAttributes(
            webRequest: WebRequest?,
            includeStackTrace: Boolean): MutableMap<String, Any> {
        val errorAttributes =
                super.getErrorAttributes(webRequest, includeStackTrace)

        // format the timestamp
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