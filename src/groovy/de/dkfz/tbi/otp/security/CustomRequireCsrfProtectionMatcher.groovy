package de.dkfz.tbi.otp.security

import org.springframework.security.web.util.matcher.RequestMatcher

import javax.servlet.http.HttpServletRequest

/***
 * This is a workaround class for multipart data because there was a problem with the order filter.
 * Multipart data is now secured using the grails withForm syntax.
 * Also the grails web console is using its own CSRF implementation.
 */
class CustomRequireCsrfProtectionMatcher implements RequestMatcher {

    @Override
    boolean matches(HttpServletRequest request) {
        if (request.isGet()) {
            return false
        }
        if (request.contentType?.contains("multipart/form-data")) {
            return false
        }
        if ((request.forwardURI - request.contextPath) == "/console/execute") {
            return false
        }

        return true
    }

}