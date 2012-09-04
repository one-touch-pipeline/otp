package de.dkfz.tbi.otp.utils

import grails.util.Environment

import java.io.IOException

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean

/**
 * Filter to give a configured user access to the web application in development mode.
 *
 * The idea behind this backdoor is that in development mode the developer does not have
 * to login. For that the filter creates a UsernamePasswordAuthenticationToken which is
 * authenticated and has the authorities ROLE_USER and ROLE_ADMIN. With other words: the
 * filter allows full access to the application.
 *
 * As that is quite a security risk the filter ensures that it is only used in development
 * mode. Nevertheless the configuration should also ensure that the filter is only added in
 * development mode, so that it does not even get loaded in production mode. Furthermore it
 * would be best to drop the compiled class from the war file.
 *
 * The filter is controlled by the config options "useBackdoor" (boolean) and "backdoorUser" (String).
 * The first one controls whether the filter is enabled at all, the second one the user to use.
 *
 */
@Component("backdoorFilter")
@Scope("singleton")
class BackdoorFilter extends GenericFilterBean {
    /**
     * Dependency Injection of grailsApplication
     */
    @Autowired
    GrailsApplication grailsApplication
    /**
     * Ensures, that the filter is only applied once per request
     */
    static final String FILTER_APPLIED = "__otp_security_caf_applied"
    /**
     * The filter injects a user with the ROLE_USER and ROLE_ADMIN authorities
     */
    private List<GrantedAuthority> authorities = [new GrantedAuthorityImpl("ROLE_USER"), new GrantedAuthorityImpl("ROLE_ADMIN")]

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req
        HttpServletResponse response = (HttpServletResponse) res

        if (request.getAttribute(FILTER_APPLIED)) {
            // ensure that filter is only applied once per request
            chain.doFilter(request, response)
            return
        }
        HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, response)

        if (!Environment.isDevelopmentMode()) {
            // if we are not in development mode, the filter is inactive
            chain.doFilter(request, response)
            return
        }
        if (grailsApplication.config.otp.security.useBackdoor instanceof ConfigObject || !grailsApplication.config.otp.security.useBackdoor ||
                grailsApplication.config.otp.security.backdoorUser instanceof ConfigObject) {
            // backdoor filter disabled by configuration
            chain.doFilter(request, response)
            return
        }

        try {
            Authentication authentication = SecurityContextHolder.context.authentication
            if (!authentication || !authentication.isAuthenticated()) {
                SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(grailsApplication.config.otp.security.backdoorUser, null, authorities)
            }
            chain.doFilter(holder.getRequest(), holder.getResponse())

        } finally {
            request.removeAttribute(FILTER_APPLIED)
        }
    }
}
