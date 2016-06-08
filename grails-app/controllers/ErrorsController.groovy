import javax.servlet.http.HttpServletResponse

class ErrorsController {
    def springSecurityService

    def error403 = {
        response.setStatus HttpServletResponse.SC_FORBIDDEN
        if (springSecurityService.isAjax(request)) {
            render springSecurityService.isLoggedIn().toString()
            return
        } else {
            [authenticated: springSecurityService.isLoggedIn()]
        }
    }

    def error404 = {
        if (springSecurityService.isAjax(request)) {
            render request.forwardURI
            return
        } else {
            [notFound: request.forwardURI]
        }
    }

    def error500 = {
        def exception = request.getAttribute('exception')
        String stackTrace = ''
        if (exception) {
            StringWriter stackTraceBuffer = new StringWriter()
            PrintWriter stackTracePrintWriter = new PrintWriter(stackTraceBuffer)
            exception.printStackTrace(stackTracePrintWriter)
            stackTrace = stackTraceBuffer.toString()
        }
        String digest = stackTrace.encodeAsMD5()
        log.error("Displaying exception with digest ${digest}.")
        if (springSecurityService.isAjax(request)) {
            render digest
            return
        } else {
            [
                    code: digest,
                    message: exception?.message,
                    stackTrace: stackTrace,
            ]
        }
    }
}
