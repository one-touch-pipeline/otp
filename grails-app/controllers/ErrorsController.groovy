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
        String digest = ''
        if (exception) {
            if (exception.message) {
                digest = exception.message
            }
            exception.stackTrace.each {
                digest += it.toString()
            }
        }
        digest = digest.encodeAsMD5()
        if (springSecurityService.isAjax(request)) {
            render digest
            return
        } else {
            [code: digest]
        }
    }
}
