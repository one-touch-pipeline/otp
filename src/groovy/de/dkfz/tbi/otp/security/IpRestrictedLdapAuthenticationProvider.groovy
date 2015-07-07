package de.dkfz.tbi.otp.security

import grails.util.Holders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.util.IpAddressMatcher

/*
 * This provider logs in the user using the regular LdapAuthenticationProvider,
 * if they logged in from outside the DKFZ network, it removes their admin role
 * and replaces it with the regular user role.
 * NB: since OTP is usually run behind a reserve proxy, the servlet container (Tomcat)
 * must be configured to replace the proxy address with the real client address
 * The internal IP addresses must be configured in .otp.properties as a whitespace separated list;
 * IPv4, IPv6 addresses are allowed as well as address ranges in CIDR notation.
 */

class IpRestrictedLdapAuthenticationProvider implements AuthenticationProvider {
    @Autowired
    LdapAuthenticationProvider ldapAuthProvider

    List<IpAddressMatcher> internalAddressMatchers = []

    IpRestrictedLdapAuthenticationProvider() {
        (Holders.config.otp.security.internalNetwork as String).split(/\s/).each {
           internalAddressMatchers.add(new IpAddressMatcher(it))
        }
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        authentication = ldapAuthProvider.authenticate(authentication)

        WebAuthenticationDetails wad = authentication.details as WebAuthenticationDetails
        String userIpAddress = wad.remoteAddress
        boolean isInternal = internalAddressMatchers.any { it.matches(userIpAddress) }

        if(!isInternal && authentication.authorities*.authority.contains("ROLE_ADMIN")) {
            Set<? extends GrantedAuthority> authorities = authentication.authorities as Set
            authorities.removeAll { it.authority == "ROLE_ADMIN" }
            authorities.add(new GrantedAuthorityImpl("ROLE_USER"))

            authentication = new UsernamePasswordAuthenticationToken(
                    authentication.principal,
                    authentication.credentials,
                    authorities
            )
            authentication.setDetails(authentication.details)
        }
        return authentication
    }

    @Override
    boolean supports(Class<? extends Object> aClass) {
        return (aClass == UsernamePasswordAuthenticationToken)
    }
}
