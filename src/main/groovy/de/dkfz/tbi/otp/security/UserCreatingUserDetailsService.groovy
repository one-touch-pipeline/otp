/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.security

import grails.core.GrailsApplication
import grails.plugin.springsecurity.userdetails.GormUserDetailsService
import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException

import de.dkfz.tbi.otp.administration.LdapUserDetails
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.utils.CollectionUtils

class UserCreatingUserDetailsService extends GormUserDetailsService {

    @Autowired
    GrailsApplication grailsApplication

    @Override
    @Transactional(readOnly=false, noRollbackFor=[IllegalArgumentException, UsernameNotFoundException])
    UserDetails loadUserByUsername(String username, boolean loadRoles) throws UsernameNotFoundException {
        User user = CollectionUtils.atMostOneElement(User.findAllByUsername(username))
        if (!user) {
            LdapUserDetails ldapUserDetails = grailsApplication.mainContext.ldapService.getLdapUserDetailsByUsername(username)
            if (!ldapUserDetails || !ldapUserDetails.mail || User.findAllByEmail(ldapUserDetails.mail)) {
                throw new FailedToCreateUserException("There is a problem with your account. Please contact " +
                    "${grailsApplication.mainContext.processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL)}.")
            }
            user = grailsApplication.mainContext.userService.createUser(ldapUserDetails.username, ldapUserDetails.mail, ldapUserDetails.realName)
        }
        Collection<GrantedAuthority> authorities = loadAuthorities(user, username, loadRoles)
        createUserDetails user, authorities
    }
}
