package hu.balazs.sido.reh.controller

import hu.balazs.sido.reh.common.getErrorCauses
import hu.balazs.sido.reh.model.RestErrorCause
import hu.balazs.sido.reh.model.RestErrorResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest

@Component
class RestErrorAttributes : DefaultErrorAttributes() {

    @Value("\${rest.api.version}")
    private lateinit var restApiVersion: String

    override fun getErrorAttributes(
            webRequest: WebRequest?,
            includeStackTrace: Boolean): MutableMap<String, Any> {
        val errorAttributes =
                super.getErrorAttributes(webRequest, includeStackTrace)

        val response = RestErrorResponse(
                apiVersion = restApiVersion,
                status = errorAttributes["status"] as Int,
                message = errorAttributes.getOrDefault(
                        "message",
                        "Error while performing request") as String,
                path = errorAttributes["path"] as String,
                causes = getError(webRequest).getErrorCauses()
        )
        return response.toAttributeMap()
    }

}