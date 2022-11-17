/*
 * Copyright 2011-2020 The OTP authors
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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.utils.MailHelperService

/**
 * Scheduled job to find and report users which can be resolved to an object in LDAP.
 *
 * It only considers enabled users with a username.
 */
@Component
@Slf4j
class UnknownLdapUsersJob extends AbstractScheduledJob {

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    IdentityProvider identityProvider

    static final String MAIL_SUBJECT = "Found unknown LDAP users in the OTP user management"

    static final String MAIL_CONTEXT = """\
    |Unknown users are OTP users with a username which can not be resolved to an LDAP object.
    |If this is a persistent issue, remove the username or disable them in OTP.
    |""".stripMargin()

    static List<User> getUsersToCheck() {
        return User.createCriteria().list {
            eq("enabled", true)
            isNotNull("username")
        } as List<User>
    }

    static String buildMailBody(List<User> unresolvableUsers) {
        String content = """\
        |${MAIL_CONTEXT}
        |
        |The following users could not be resolved:
        |""".stripMargin()
        unresolvableUsers.sort { it.username }.each { User user ->
            content += "${user}\n"
        }
        return content
    }

    void sendNotification(String body) {
        mailHelperService.sendEmailToTicketSystem(MAIL_SUBJECT, body)
    }

    List<User> getUsersThatCanNotBeFoundInLdap(List<User> users) {
        return users.findAll { User user -> !identityProvider.exists(user) }
    }

    @Override
    void wrappedExecute() {
        List<User> unresolvableUsers = getUsersThatCanNotBeFoundInLdap(usersToCheck)
        if (unresolvableUsers) {
            sendNotification(buildMailBody(unresolvableUsers))
        } else {
            log.info("Job executed, but no unresolved users found")
        }
    }
}
