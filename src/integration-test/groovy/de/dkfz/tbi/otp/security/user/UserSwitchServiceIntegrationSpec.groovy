/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.security.user

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.PseudoEnvironment
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class UserSwitchServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    UserSwitchService securityService

    void "getWhitelistedEnvironments, returns all Environments but PRODUCTION"() {
        given:
        securityService = new UserSwitchService()

        expect:
        TestCase.assertContainSame(securityService.whitelistedEnvironments, [
                PseudoEnvironment.PRODUCTION_TEST,
                PseudoEnvironment.DEVELOPMENT,
                PseudoEnvironment.TEST,
                PseudoEnvironment.WORKFLOW_TEST,
        ])
    }

    @Unroll
    void "isToBeBlockedBecauseOfSwitchedUser, all test cases, #environment, #expectedNormal, #expectedSwitched"() {
        given:
        securityService = new UserSwitchService(
                configService: Mock(TestConfigService) {
                    1 * getPseudoEnvironment() >> environment
                }
        )
        createUserAndRoles()

        when:
        boolean resultNormal = securityService.toBeBlockedBecauseOfSwitchedUser

        then:
        resultNormal == expectedNormal

        when:
        boolean resultSwitched
        doAsSwitchedToUser(USER) {
            resultSwitched = securityService.toBeBlockedBecauseOfSwitchedUser
        }

        then:
        resultSwitched == expectedSwitched

        where:
        environment                   || expectedNormal | expectedSwitched
        PseudoEnvironment.DEVELOPMENT || false          | false
        PseudoEnvironment.PRODUCTION  || false          | true
    }

    void "assertNotSwitchedUser, throws Exception when action is blocked for switched users"() {
        given:
        securityService = new UserSwitchService(
                configService: Mock(TestConfigService) {
                    1 * getPseudoEnvironment() >> PseudoEnvironment.PRODUCTION
                }
        )
        createUserAndRoles()

        when:
        securityService.ensureNotSwitchedUser()

        then:
        true

        when:
        doAsSwitchedToUser(USER) {
            securityService.ensureNotSwitchedUser()
        }

        then:
        thrown(SwitchedUserDeniedException)
    }
}
