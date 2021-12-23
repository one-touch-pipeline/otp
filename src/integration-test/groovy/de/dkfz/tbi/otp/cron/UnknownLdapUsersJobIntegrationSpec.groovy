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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Ignore
import spock.lang.Specification

import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService

@Rollback
@Integration
class UnknownLdapUsersJobIntegrationSpec extends Specification implements DomainFactoryCore {

    void "getUsersToCheck, only returns enabled users with a username"() {
        given:
        List<User> createdUsers = [
                [null, true],
                [null, false],
                ["username.a", true],
                ["username.b", false],
        ].collect { List<?> properties ->
            DomainFactory.createUser(username: properties[0], enabled: properties[1])
        }

        List<User> expected = [createdUsers[2]]
        List<User> result

        when:
        result = UnknownLdapUsersJob.usersToCheck

        then:
        expected == result
    }

    void "getUsersThatCanNotBeFoundInLdap, only returns users that return false from existsInLdap"() {
        given:
        List<User> usersToBeFound = ["a", "b", "c"].collect { String username ->
            return DomainFactory.createUser(username: username)
        }
        List<User> usersToNotBeFound = ["d", "e"].collect { String username ->
            return DomainFactory.createUser(username: username)
        }
        UnknownLdapUsersJob job = new UnknownLdapUsersJob([
                ldapService: Mock(LdapService) {
                    5 * existsInLdap(_) >> { User user ->
                        return (user in usersToBeFound)
                    }
                    0 * _
                },
        ])

        List<User> result

        when:
        result = job.getUsersThatCanNotBeFoundInLdap(usersToBeFound + usersToNotBeFound)

        then:
        result == usersToNotBeFound
    }

    @Ignore
    void "wrappedExecute, sends a mail containing every unresolvable user"() {
        given:
        ["A", "B", "C"].collect { String username ->
            return DomainFactory.createUser(username: username)
        }
        List<User> usersToNotBeFound = ["D", "E"].collect { String username ->
            return DomainFactory.createUser(username: username)
        }
        UnknownLdapUsersJob job = new UnknownLdapUsersJob([
                processingOptionService: new ProcessingOptionService(),
                ldapService: Mock(LdapService) {
                    5 * existsInLdap(_) >> { User user ->
                        return !(user in usersToNotBeFound)
                    }
                    0 * _
                },
                mailHelperService: Mock(MailHelperService) {
                    1 * sendEmailToTicketSystem(_, { usersToNotBeFound.every { User user -> it.contains(user.username) } }) >> { }
                },
        ])

        expect:
        job.wrappedExecute()
    }

    @Ignore
    void "wrappedExecute, only writes out a log message when no unresolved users were found"() {
        given:
        DomainFactory.createUser(username: "username", enabled: true)
        UnknownLdapUsersJob job = new UnknownLdapUsersJob([
                processingOptionService: new ProcessingOptionService(),
                ldapService: Mock(LdapService) {
                    1 * existsInLdap(_) >> { User user ->
                        return true
                    }
                    0 * _
                },
                mailHelperService: Mock(MailHelperService) {
                    0 * sendEmailToTicketSystem(_, _) >> { }
                },
        ])

        expect:
        job.wrappedExecute()
    }
}
