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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Principal

@Service
class UserCreatingUserDetailsService implements UserDetailsService {

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    UserService userService

    @Autowired
    IdentityProvider identityProvider

    @CompileDynamic
    @Override
    @Transactional(readOnly = false, noRollbackFor = [IllegalArgumentException, UsernameNotFoundException])
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = CollectionUtils.atMostOneElement(User.findAllByUsername(username))
        if (!user) {
            IdpUserDetails idpUserDetails = identityProvider.getIdpUserDetailsByUsername(username)
            if (!idpUserDetails || !idpUserDetails.mail) {
                throw new FailedToCreateUserException("There is a problem with your account. Please contact " +
                        "${processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL)}.")
            }
            user = userService.createUser(idpUserDetails.username, idpUserDetails.mail, idpUserDetails.realName)
        }
        return new Principal(user.username, user.authorities.collect { new SimpleGrantedAuthority(it.authority) }, user.enabled)
    }
}
