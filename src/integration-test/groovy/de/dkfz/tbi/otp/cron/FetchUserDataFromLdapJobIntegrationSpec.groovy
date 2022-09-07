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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.security.user.identityProvider.LdapService
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.User

@Rollback
@Integration
class FetchUserDataFromLdapJobIntegrationSpec extends Specification {

    FetchUserDataFromLdapJob job = new FetchUserDataFromLdapJob()

    void "wrappedExecute, should update the realName and email when it was changed on ldap"() {
        given:
        final String newName = "new name"
        final String newMail = "new@mail.de"
        final User otpUser = DomainFactory.createUser()

        job.ldapService = Mock(LdapService) {
            1 * getLdapUserDetailsByUserList(_) >> { List<User> otpUsers ->
                return [
                        new IdpUserDetails([
                                username: otpUsers.flatten().first().username,
                                realName: newName,
                                mail: newMail,
                        ]),
                ]
            }
        }

        when:
        job.wrappedExecute()

        then:
        noExceptionThrown()
        User.findAllByUsername(otpUser.username)[0].email == newMail
        User.findAllByUsername(otpUser.username)[0].realName == newName
    }
}
