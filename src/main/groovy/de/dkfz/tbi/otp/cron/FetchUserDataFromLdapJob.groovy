/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.cron

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.LdapUserDetails
import de.dkfz.tbi.otp.security.User

/**
 * Scheduled job to update user information from the ldap.
 */
@SuppressWarnings("AnnotationsForJobs")
@Scope("singleton")
@Component
@Slf4j
class FetchUserDataFromLdapJob extends ScheduledJob {

    @Autowired
    LdapService ldapService

    @Override
    void wrappedExecute() {
        List<User> otpUsers = User.findAllByEnabled(true)
        List<LdapUserDetails> ldapUserDetails = ldapService.getLdapUserDetailsByUserList(otpUsers)

        otpUsers.each { User otpUser ->
            LdapUserDetails syncedLdapUser = ldapUserDetails.find { it.username == otpUser.username }

            if (syncedLdapUser) {
                otpUser.realName = syncedLdapUser.realName
                if (syncedLdapUser.mail) {
                    otpUser.email = syncedLdapUser.mail
                }
                otpUser.save(flush: true)
            }
        }
    }
}
