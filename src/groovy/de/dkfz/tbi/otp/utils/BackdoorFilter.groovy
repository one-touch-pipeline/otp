package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.config.*
import grails.util.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.security.authentication.*
import org.springframework.security.core.*
import org.springframework.security.core.authority.*
import org.springframework.security.core.context.*
import org.springframework.security.web.context.*
import org.springframework.stereotype.*
import org.springframework.web.filter.*

import javax.servlet.*
import javax.servlet.http.*

/**
 * Filter to give a configured user access to the web application in development mode.
 *
 * The idea behind this backdoor is that in development mode the developer does not have
 * to login. For that the filter creates a UsernamePasswordAuthenticationToken which is
 * authenticated and has the authority ROLE_ADMIN. With other words: the
 * filter allows full access to the application.
 *
 * As that is quite a security risk the filter ensures that it is only used in development
 * mode. Nevertheless the configuration should also ensure that the filter is only added in
 * development mode, so that it does not even get loaded in production mode. Furthermore it
 * would be best to drop the compiled class from the war file.
 *
 * The filter is controlled by the config options {@link OtpProperty#DEVEL_USE_BACKDOOR} and
 * {@link OtpProperty#DEVEL_BACKDOOR_USER}.
 * The first one controls whether the filter is enabled at all, the second one the user to use.
 */
@Component("backdoorFilter")
@Scope("singleton")
class BackdoorFilter extends GenericFilterBean {

    @Autowired
    ConfigService configService
    /**
     * Ensures, that the filter is only applied once per request
     */
    static final String FILTER_APPLIED = "__otp_security_caf_applied"
    /**
     * The filter injects a user with the ROLE_ADMIN authority
     */
    private List<GrantedAuthority> authorities = [
            new SimpleGrantedAuthority(de.dkfz.tbi.otp.security.Role.ROLE_ADMIN),
    ]

    @Override
    void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
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
        if (!configService.useBackdoor() || !configService.getBackdoorUser()) {
            // backdoor filter disabled by configuration
            chain.doFilter(request, response)
            return
        }

        try {
            Authentication authentication = SecurityContextHolder.context.authentication
            if (!authentication || !authentication.isAuthenticated()) {
                Principal principal = new Principal(username: configService.getBackdoorUser())
                SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities)
            }
            chain.doFilter(holder.getRequest(), holder.getResponse())

        } finally {
            request.removeAttribute(FILTER_APPLIED)
        }
    }
}
