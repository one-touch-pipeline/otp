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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.RolesService
import de.dkfz.tbi.otp.security.SecurityService

@Rollback
@Integration
class MmmlServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    void "hideSampleIdentifiersForCurrentUser, all cases"() {
        given:
        MmmlService mmmlService = new MmmlService(
                rolesService   : Mock(RolesService) {
                    _ * isAdministrativeUser(_) >> isAdmin
                },
                securityService: Mock(SecurityService) {
                    _ * getCurrentUserAsUser() >> { DomainFactory.createUser() }
                },
        )

        Map properties = [:]
        if (projectBlacklisted) {
            properties << [name: MmmlService.PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.first()]
        }
        Project project = createProject(properties)

        expect:
        mmmlService.hideSampleIdentifiersForCurrentUser(project) == expected

        where:
        isAdmin | projectBlacklisted || expected
        false   | false              || false
        false   | true               || true
        true    | false              || false
        true    | true               || false
    }
}
