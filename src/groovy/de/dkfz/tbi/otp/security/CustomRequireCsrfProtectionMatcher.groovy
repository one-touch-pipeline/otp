package de.dkfz.tbi.otp.security

import org.springframework.security.web.util.matcher.RequestMatcher

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

/***
 * This is a workaround class for multipart data because there was a problem with the order filter.
 * Multipart data is now secured using the grails withForm syntax.
 */
class CustomRequireCsrfProtectionMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        if (request.contentType?.contains("multipart/form-data")) {
            return false
        }
        if (request.isGet()) {
            return false
        }

        return true
    }

}