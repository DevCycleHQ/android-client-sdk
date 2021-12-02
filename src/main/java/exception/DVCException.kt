package exception

import lombok.Getter
import model.ErrorResponse
import model.HttpResponseCode

@Getter
class DVCException(
    private val httpResponseCode: HttpResponseCode,
    private val errorResponse: ErrorResponse
) : Exception()